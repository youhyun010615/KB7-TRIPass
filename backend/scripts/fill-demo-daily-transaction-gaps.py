#!/usr/bin/env python3
"""Fill no-spend gaps in the yuhyun savings-period mock timeline.

The source XLS files are intentionally not read or copied here.  The generated
amounts stay near the median of the already anonymized demo card data and the
script is deterministic/idempotent so the committed JSON remains reproducible.
"""

from __future__ import annotations

import json
from datetime import date, datetime, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CARD_PATH = ROOT / "src/main/resources/mock/demo-virtual-card-transactions.json"
BANK_PATH = ROOT / "src/main/resources/mock/demo-virtual-bank-transactions.json"
START = date(2026, 8, 26)
END = date(2027, 4, 3)

TEMPLATES = (
    ("081500", 1_800, "컴포즈커피", "커피전문점"),
    ("123500", 7_000, "오늘의한끼", "일반음식점"),
    ("184000", 4_900, "GS25", "편의점"),
    ("203000", 8_900, "쿠팡이츠", "배달서비스"),
)


def load(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8") as file:
        return json.load(file)


def save(path: Path, rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8") as file:
        json.dump(rows, file, ensure_ascii=False, indent=2)
        file.write("\n")


def date_range(start: date, end: date):
    current = start
    while current <= end:
        yield current
        current += timedelta(days=1)


def main() -> None:
    cards = load(CARD_PATH)
    bank = load(BANK_PATH)
    covered = {
        datetime.strptime(row["resUsedDate"], "%Y%m%d").date()
        for row in cards
    }
    missing = [day for day in date_range(START, END) if day not in covered]

    for index, day in enumerate(missing):
        time, amount, merchant, merchant_type = TEMPLATES[index % len(TEMPLATES)]
        date_text = day.strftime("%Y%m%d")
        approval = f"DG{day.strftime('%y%m%d')}01"
        cards.append(
            {
                "resUsedDate": date_text,
                "resUsedTime": time,
                "resUsedAmount": str(amount),
                "resCancelYN": "N",
                "resApprovalNo": approval,
                "resMemberStoreName": merchant,
                "resMemberStoreType": merchant_type,
            }
        )
        bank.append(
            {
                "resAccountTrDate": date_text,
                "resAccountTrTime": time,
                "resAccountIn": "0",
                "resAccountOut": str(amount),
                "resAfterTranBalance": "0",
                "resAccountDesc1": merchant,
                "resAccountDesc2": "체크카드",
            }
        )

    cards.sort(key=lambda row: (row["resUsedDate"], row["resUsedTime"], row["resApprovalNo"]))
    bank.sort(key=lambda row: (row["resAccountTrDate"], row["resAccountTrTime"]))

    if bank:
        first = bank[0]
        balance = (
            int(first["resAfterTranBalance"])
            - int(first["resAccountIn"])
            + int(first["resAccountOut"])
        )
        for row in bank:
            balance += int(row["resAccountIn"]) - int(row["resAccountOut"])
            row["resAfterTranBalance"] = str(balance)

    save(CARD_PATH, cards)
    save(BANK_PATH, bank)
    print(f"filled_days={len(missing)} card_rows={len(cards)} bank_rows={len(bank)}")


if __name__ == "__main__":
    main()
