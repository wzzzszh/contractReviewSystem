from __future__ import annotations

import argparse
import html
import json
import re
import time
from pathlib import Path
from urllib.parse import quote, urlencode
from urllib.request import Request, urlopen

from lxml import html as lxml_html


BASE_URL = "https://htsfwb.samr.gov.cn"
SEARCH_API = f"{BASE_URL}/api/content/SearchTemplates"
VIEW_URL = f"{BASE_URL}/View"
TYPE_NAMES = {
    1: "生活消费",
    2: "农资农业",
    3: "生产经营",
    4: "建设工程",
    5: "其他",
}


def fetch_json(url: str) -> dict:
    req = Request(url, headers={"User-Agent": "Mozilla/5.0 contract-risk-skill-builder"})
    with urlopen(req, timeout=45) as resp:
        return json.loads(resp.read().decode("utf-8"))


def fetch_text(url: str) -> str:
    req = Request(url, headers={"User-Agent": "Mozilla/5.0 contract-risk-skill-builder"})
    with urlopen(req, timeout=45) as resp:
        return resp.read().decode("utf-8", errors="replace")


def normalize_text(value: str) -> str:
    value = html.unescape(value or "")
    value = value.replace("\r\n", "\n").replace("\r", "\n")
    value = value.replace("\u00a0", " ").replace("\u3000", " ")
    lines = []
    for line in value.splitlines():
        line = re.sub(r"[ \t]+", " ", line).strip()
        if line:
            lines.append(line)
    return "\n".join(lines)


def text_content(nodes) -> str:
    chunks = []
    for node in nodes:
        chunks.append(node.text_content())
    return normalize_text("\n".join(chunks))


def first_text(doc, xpath: str) -> str:
    nodes = doc.xpath(xpath)
    if not nodes:
        return ""
    return normalize_text(nodes[0].text_content())


def extract_info(doc) -> dict:
    info = {}
    rows = doc.xpath("//div[contains(@class,'samr-view-info')]/div")
    for row in rows:
        title = first_text(row, ".//div[contains(@class,'info-title')]").rstrip("：:")
        content = first_text(row, ".//div[contains(@class,'info-content')]")
        if title:
            info[title] = content
    return info


def extract_risks(doc) -> list[dict]:
    risks = []
    for item in doc.xpath("//div[contains(@class,'samr-view-risk-item')]"):
        title = first_text(item, ".//div[contains(concat(' ',normalize-space(@class),' '),' title ')]")
        content = first_text(item, ".//div[contains(@class,'risk-discription')]")
        if title or content:
            risks.append({"title": title, "content": content})
    return risks


def extract_content_lines(doc) -> list[str]:
    content = text_content(doc.xpath("//div[contains(@class,'samr-view-content')]"))
    return [line.strip() for line in content.splitlines() if line.strip()]


def extract_clause_outline(lines: list[str], max_items: int = 35) -> list[str]:
    headings = []
    seen = set()
    patterns = [
        re.compile(r"^使用说明$"),
        re.compile(r"^合同签订提示$"),
        re.compile(r"^特别告知$"),
        re.compile(r"^风险提示$"),
        re.compile(r"^第[一二三四五六七八九十百〇零0-9]+[章节条].{0,80}$"),
        re.compile(r"^附件[一二三四五六七八九十百〇零0-9]*[:：]?.{0,80}$"),
    ]
    for line in lines:
        compact = re.sub(r"\s+", "", line)
        if len(compact) > 90:
            continue
        if any(p.match(compact) for p in patterns):
            if compact not in seen:
                headings.append(compact)
                seen.add(compact)
        if len(headings) >= max_items:
            break
    return headings


def strip_edition(title: str) -> str:
    title = title.strip()
    title = re.sub(r"（[^（）]*(?:20\d{2}|19\d{2})版[^（）]*）\s*$", "", title)
    title = re.sub(r"\([^()]*20\d{2}版[^()]*\)\s*$", "", title)
    return title.strip() or title


def safe_filename(name: str) -> str:
    name = strip_edition(name)
    name = re.sub(r"[\\/:*?\"<>|]", "", name)
    name = re.sub(r"\s+", "", name)
    return name[:80]


def md_escape(value: str) -> str:
    return value.replace("|", "\\|").strip()


def bullet_lines(items: list[str]) -> str:
    if not items:
        return "- 暂未从示范文本中提取到明确章节标题，请结合合同正文和官方风险提示进行通用审查。"
    return "\n".join(f"- {item}" for item in items)


def risk_section(risks: list[dict]) -> str:
    if not risks:
        return "暂无官方风险提示；审查时应回退到通用合同审查框架。"
    parts = []
    for idx, risk in enumerate(risks, start=1):
        title = risk["title"] or f"风险提示 {idx}"
        content = risk["content"] or "官方仅列示该风险标题，未提供展开说明。"
        parts.append(f"### {idx}. {title}\n\n{content}")
    return "\n\n".join(parts)


def review_points(outline: list[str], risks: list[dict]) -> list[str]:
    points = []
    for risk in risks:
        if risk["title"]:
            points.append(f"围绕“{risk['title']}”核查合同是否已有明确安排、责任分配和违约后果。")
    for heading in outline:
        if heading in {"使用说明", "合同签订提示", "特别告知", "风险提示"}:
            continue
        points.append(f"核查“{heading}”是否完整、清晰，并与交易实际一致。")
    deduped = []
    seen = set()
    for point in points:
        if point not in seen:
            deduped.append(point)
            seen.add(point)
    return deduped[:18]


def build_markdown(item: dict, detail: dict) -> str:
    title = strip_edition(item["Title"])
    source_url = f"{VIEW_URL}?id={item['Id']}"
    risks = detail["risks"]
    outline = detail["outline"]
    points = review_points(outline, risks)
    point_text = "\n".join(f"- {p}" for p in points) if points else "- 按通用合同审查框架核查主体、标的、价款、履行、违约、解除和争议解决。"
    warnings = [risk["title"] for risk in risks if risk["title"]]
    warning_text = "\n".join(f"- {w}" for w in warnings[:10]) if warnings else "- 暂无官方风险标题，需重点核查合同必备条款是否完整。"

    info = detail["info"]
    return f"""# {title}

## 来源信息

| 项目 | 内容 |
|------|------|
| 来源 | {source_url} |
| 合同ID | {item['Id']} |
| 原标题 | {md_escape(item['Title'])} |
| 地区 | {md_escape(item.get('Region') or '地方')} |
| 分类 | {md_escape(TYPE_NAMES.get(item.get('Type'), str(item.get('Type') or '')))} |
| 发布机关 | {md_escape(info.get('发布机关') or item.get('Department') or '')} |
| 发布年份 | {md_escape(info.get('发布年份') or (str(item.get('PublishedOn')) + '年'))} |
| 发布编号 | {md_escape(info.get('发布编号') or '')} |

## 适用场景

{normalize_text(item.get('Brief') or '适用于该示范文本对应的地方合同交易场景。')}

## 示范文本结构要点

{bullet_lines(outline)}

## 必审条款

{point_text}

## 官方风险提示

{risk_section(risks)}

## 高风险预警

{warning_text}
"""


def collect_listing(start_page: int, end_page: int | None) -> tuple[list[dict], int]:
    first_url = f"{SEARCH_API}?{urlencode({'key': '', 'y': '6', 'loc': 'true', 'p': start_page})}"
    first = fetch_json(first_url)
    total_page = int(first["TotalPage"])
    if end_page is None:
        end_page = total_page
    items = list(first["Data"])
    for page in range(start_page + 1, end_page + 1):
        url = f"{SEARCH_API}?{urlencode({'key': '', 'y': '6', 'loc': 'true', 'p': page})}"
        data = fetch_json(url)
        items.extend(data["Data"])
        time.sleep(0.25)
    return items, total_page


def collect_detail(contract_id: str) -> dict:
    page = fetch_text(f"{VIEW_URL}?id={quote(contract_id)}")
    doc = lxml_html.fromstring(page)
    lines = extract_content_lines(doc)
    return {
        "info": extract_info(doc),
        "risks": extract_risks(doc),
        "outline": extract_clause_outline(lines),
    }


def existing_numbers(output_dir: Path) -> set[int]:
    numbers = set()
    for path in output_dir.glob("*.md"):
        match = re.match(r"^(\d+)-", path.name)
        if match:
            numbers.add(int(match.group(1)))
    return numbers


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--start-page", type=int, default=7)
    parser.add_argument("--end-page", type=int)
    parser.add_argument("--start-number", type=int)
    parser.add_argument("--sleep", type=float, default=0.35)
    args = parser.parse_args()

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    items, total_page = collect_listing(args.start_page, args.end_page)
    next_number = args.start_number
    if next_number is None:
        nums = existing_numbers(output_dir)
        next_number = max(nums or {0}) + 1

    created = []
    for item in items:
        detail = collect_detail(item["Id"])
        filename = f"{next_number:02d}-{safe_filename(item['Title'])}.md"
        path = output_dir / filename
        path.write_text(build_markdown(item, detail), encoding="utf-8", newline="\n")
        created.append(path.name)
        next_number += 1
        time.sleep(args.sleep)

    manifest = {
        "source": BASE_URL,
        "scope": f"地方合同示范文本，2020年以后，第{args.start_page}页至第{args.end_page or total_page}页",
        "count": len(created),
        "files": created,
    }
    (output_dir / "_samr_local_page7_manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2),
        encoding="utf-8",
        newline="\n",
    )
    print(json.dumps({"created": len(created), "first": created[:3], "last": created[-3:]}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
