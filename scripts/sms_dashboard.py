#!/usr/bin/env python3
"""
SMS Backup XML Dashboard v2
- Bank SMS vs Notification classification (SmsNotificationClassifier)
- Duplicate detection (same sender + day + amount)
- Verification/OTP filtering
- Category classification (CategorizationEngine)
- Amount-group drill-down
- Monthly breakdown with all features
"""

import re, html, json, sys, os
from datetime import datetime, timedelta
from http.server import HTTPServer, SimpleHTTPRequestHandler
from xml.etree import ElementTree as ET
from urllib.parse import urlparse
from collections import defaultdict

# ── SmsParser.kt regex ──
DEBIT_KW = re.compile(r'(?i)(?:debited|debit|spent|paid|withdrawn|purchase|sent|transferred)')
CREDIT_KW = re.compile(r'(?i)(?:credited|credit|received|deposited|refund)')
BARE_AMOUNT = re.compile(r'(?i)(?:rs\.?|inr|₹)\s*([\d,]+\.?\d*)')
AMOUNT_AFTER_DEBIT = re.compile(r'(?i)(?:debited|debit|spent|paid|withdrawn|purchase|sent|transferred).*?(?:rs\.?|inr|₹)\s*([\d,]+\.?\d*)')
AMOUNT_BEFORE_DEBIT = re.compile(r'(?i)(?:rs\.?|inr|₹)\s*([\d,]+\.?\d*).*?(?:debited|debit|spent|paid|withdrawn|purchase)')
AMOUNT_AFTER_CREDIT = re.compile(r'(?i)(?:credited|credit|received|deposited|refund).*?(?:rs\.?|inr|₹)\s*([\d,]+\.?\d*)')
AMOUNT_BEFORE_CREDIT = re.compile(r'(?i)(?:rs\.?|inr|₹)\s*([\d,]+\.?\d*).*?(?:credited|credit|received|deposited|refund)')

MERCHANT_PATTERNS = [
    re.compile(r'(?i)(?:at|to|from|for|via)\s+([A-Za-z0-9\s&.\'-]+?)(?:\s+(?:on|ref|txn|via|card|a/c|account|upi|neft|imps|rtgs))'),
    re.compile(r'(?i)(?:at|to|from)\s+([A-Za-z0-9\s&.\'-]+?)(?:\s+on\s+)'),
    re.compile(r'(?i)(?:UPI|NEFT|IMPS|RTGS).*?(?:to|from)\s+([A-Za-z0-9\s&.\'-]+?)(?:\s|\.|$)'),
]
REF_PAT = re.compile(r'(?i)(?:ref|reference|txn|transaction)\s*(?:no|number|id|#)?[:.\s]*([A-Za-z0-9]+)')

# ── ReminderFromSmsCreator.kt ──
REMINDER_KW = ["due date", "payment due", "due on", "due by", "expiry", "expire", "expiring", "expires", "will expire", "renewal", "renew", "up for renewal", "emi", "insurance due", "overdue", "subscription", "subscription end", "subscription expiry", "validity end", "plan expire"]
MONTHLY_KW = ["emi", "monthly"]
REMINDER_AMT = re.compile(r'(?i)(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)')
REMINDER_DATE = re.compile(r'(?i)(?:due\s+(?:on|by|date)\s*[:.]?\s*|expires?\s+on\s+|expiry\s*[:.]?\s*)(\d{1,2})(?:st|nd|rd|th)?\s*[-/]\s*([a-zA-Z]{3,9}|\d{1,2})\s*[-/]\s*(\d{4})')

# ── SmsNotificationClassifier.kt ──
OTP_KW = ["otp", "one time password", "verification code", "verification pin", "code is", "is your otp", "do not share", "authenticate"]
RECHARGE_KW = ["recharge", "recharged", "top-up", "top up", "data pack", "plan activated", "pack activated", "talktime", "validity extended", "recharge successful", "prepaid recharge"]
EXPIRY_KW = ["expired", "expiry", "expiring", "will expire", "expires on", "due date", "renewal", "renew", "overdue", "payment due", "insurance due", "emi due", "subscription end", "validity end", "plan expire", "will be deactivated"]
PROMO_KW = ["offer", "cashback", "discount", "coupon", "sale", "win", "congratulations", "lucky", "reward", "bonus", "flat rs", "off on", "get up to", "limited time", "hurry", "shop now", "exclusive deal", "special offer", "save rs"]
DELIVERY_KW = ["delivered", "out for delivery", "shipped", "dispatched", "order placed", "order confirmed", "tracking", "arriving", "package", "delivery expected", "your order", "shipment"]
APPT_KW = ["appointment", "booking confirmed", "scheduled", "booking slot", "your visit", "check-in", "reservation", "consultation", "meeting confirmed", "session booked"]

# ── CategorizationEngine.kt keywords ──
CATEGORY_KEYWORDS = {
    "Salary": ["salary", "wages", "payroll", "stipend", "honorarium"],
    "Food": ["food", "restaurant", "cafe", "zomato", "swiggy", "dominos", "pizza", "burger", "meal", "lunch", "dinner", "breakfast", "grocery", "groceries", "supermarket", "bigbasket", "dmart", "reliance fresh"],
    "Shopping": ["shopping", "amazon", "flipkart", "myntra", "ajio", "mall", "store", "mart", "clothing", "fashion", "electronics", "lifestyle", "westside", "pantaloons"],
    "Transport": ["transport", "uber", "ola", "rapido", "metro", "bus", "train", "flight", "petrol", "diesel", "fuel", "parking", "toll", "irctc", "makemytrip", "redbus"],
    "Bills": ["bill", "electricity", "water", "gas", "internet", "broadband", "mobile", "recharge", "postpaid", "prepaid", "wifi", "dth", "tata sky", "dish tv", "jio", "airtel", "bsnl", "vodafone"],
    "Entertainment": ["entertainment", "movie", "netflix", "prime", "hotstar", "spotify", "youtube", "gaming", "bookmyshow", "pvr", "inox", "concert", "show"],
    "Health": ["health", "hospital", "doctor", "medicine", "pharmacy", "medical", "insurance", "apollo", "pharmeasy", "netmeds", "1mg", "clinic", "diagnostic", "lab", "test"],
    "Transfer": ["transfer", "neft", "imps", "rtgs", "upi", "sent", "received", "paytm", "phonepe", "gpay", "google pay", "bhim"],
    "Investment": ["investment", "mutual fund", "sip", "stocks", "shares", "trading", "zerodha", "groww", "upstox", "angel", "fd", "fixed deposit", "rd", "recurring deposit", "ppf", "nps", "lic"],
}

# Known bank sender prefixes
BANK_SENDER_PREFIXES = ["AX-", "VK-", "VM-", "JM-", "JD-", "JX-", "JK-", "AD-", "BP-", "CP-", "VD-", "BZ-", "VA-", "BH-", "AZ-", "TM-", "VK-", "VM-"]
MONTH_MAP = {k: i+1 for i,k in enumerate(["jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"])}


def classify_notification(body):
    lower = body.lower()
    if any(k in lower for k in OTP_KW): return "OTP"
    if any(k in lower for k in EXPIRY_KW): return "EXPIRY"
    if any(k in lower for k in RECHARGE_KW): return "RECHARGE"
    if any(k in lower for k in DELIVERY_KW): return "DELIVERY"
    if any(k in lower for k in APPT_KW): return "APPOINTMENT"
    if any(k in lower for k in PROMO_KW): return "PROMOTION"
    return "OTHER"

def is_bank_sender(sender):
    return any(sender.startswith(p) for p in BANK_SENDER_PREFIXES) or any(k in sender.lower() for k in ["bank", "icici", "sbi", "hdfc", "axis", "kotak", "mahabank", "yesbank", "pnb", "bob", "union"])

def is_bank_sms(body, sender):
    lower = body.lower()
    has_bank_ref = any(x in lower for x in ["a/c", "account", "acct", "xxxx", "credited", "debited", "available bal", "avl bal", "bal is", "acc xx"])
    has_bank_sender = is_bank_sender(sender)
    return has_bank_ref or has_bank_sender

def extract_amount_typed(body):
    m = AMOUNT_AFTER_DEBIT.search(body)
    if m: return float(m.group(1).replace(",","")), "EXPENSE"
    m = AMOUNT_BEFORE_DEBIT.search(body)
    if m: return float(m.group(1).replace(",","")), "EXPENSE"
    m = AMOUNT_AFTER_CREDIT.search(body)
    if m: return float(m.group(1).replace(",","")), "INCOME"
    m = AMOUNT_BEFORE_CREDIT.search(body)
    if m: return float(m.group(1).replace(",","")), "INCOME"
    m = BARE_AMOUNT.search(body)
    if m:
        amt = float(m.group(1).replace(",",""))
        if amt > 0:
            lower = body.lower()
            # If bare amount match but no debit/credit keyword → low confidence
            if DEBIT_KW.search(body): return amt, "EXPENSE"
            if CREDIT_KW.search(body): return amt, "INCOME"
            # Check for UPI/sent patterns
            if any(x in lower for x in ["upi", "neft", "imps", "rtgs", "sent", "transferred", "paid"]): return amt, "EXPENSE"
            if any(x in lower for x in ["refund", "salary"]): return amt, "INCOME"
            return amt, None  # untyped / low confidence
    return None, None

def categorize(body, merchant):
    text = (body + " " + merchant).lower()
    for cat, kws in CATEGORY_KEYWORDS.items():
        for kw in kws:
            if kw in text:
                return cat
    # Sender-based heuristics
    if any(x in text for x in ["salary", "payroll"]): return "Salary"
    if any(x in text for x in ["mutual fund", "sip", "zerodha", "groww"]): return "Investment"
    if "recharge" in text: return "Bills"
    return "Other"

def extract_merchant(body):
    for pat in MERCHANT_PATTERNS:
        m = pat.search(body)
        if m: return m.group(1).strip()
    return ""

def extract_ref(body):
    m = REF_PAT.search(body)
    return m.group(1) if m else ""

def analyze_reminder(body, sender, ts_ms):
    lower = body.lower()
    if "otp" in lower or "one time password" in lower: return None
    matched = next((kw for kw in REMINDER_KW if kw in lower), None)
    if not matched: return None

    if "emi" in lower: title = f"EMI Due - {sender}"
    elif "renewal" in lower or "renew" in lower: title = f"Renewal Due - {sender}"
    elif "expir" in lower: title = f"Expiry Alert - {sender}"
    elif "subscription" in lower: title = f"Subscription Due - {sender}"
    else: title = f"Payment Due - {sender}"

    recurrence = "MONTHLY" if any(k in lower for k in MONTHLY_KW) else "NONE"
    amount = None
    m = REMINDER_AMT.search(body)
    if m:
        try: amount = float(m.group(1).replace(",",""))
        except: pass

    trigger_time = None
    dm = REMINDER_DATE.search(body)
    if dm:
        day = int(dm.group(1)); ms = dm.group(2); yr = int(dm.group(3))
        month = int(ms) if len(ms)<=2 else MONTH_MAP.get(ms.lower()[:3])
        if month:
            try: trigger_time = int(datetime(yr, month, day).timestamp()*1000)
            except: pass
    if not trigger_time: trigger_time = int(ts_ms) + 3*24*60*60*1000

    return {"title": title, "description": body[:200], "amount": amount, "type": "EXPENSE",
            "recurrence": recurrence, "triggerTime": trigger_time,
            "triggerDateStr": datetime.fromtimestamp(trigger_time/1000).strftime("%Y-%m-%d %H:%M"),
            "matchedKeyword": matched, "sender": sender, "smsDate": int(ts_ms),
            "smsDateStr": datetime.fromtimestamp(int(ts_ms)/1000).strftime("%Y-%m-%d %H:%M"), "smsBody": body}


def parse_xml_file(xml_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()

    all_sms = []
    transactions = []
    reminders = []
    low_confidence = []
    duplicates = []
    notifications = []

    raw_txns = []
    seen_keys = defaultdict(list)

    for elem in root.findall("sms"):
        sender = elem.get("address","")
        body = html.unescape(elem.get("body",""))
        date_str = elem.get("date","0")
        ts = int(date_str)
        date_obj = datetime.fromtimestamp(ts/1000)
        date_str_fmt = date_obj.strftime("%Y-%m-%d %H:%M")
        day_key = date_obj.strftime("%Y-%m-%d")

        all_sms.append({"sender": sender, "body": body[:200], "date": ts, "dateStr": date_str_fmt})

        notif_cat = classify_notification(body)
        is_bank = is_bank_sms(body, sender)
        sms_info = {"sender": sender, "body": body, "date": ts, "dateStr": date_str_fmt, "dayKey": day_key,
                    "notificationCategory": notif_cat, "isBank": is_bank}

        if notif_cat == "OTP":
            sms_info["txnType"] = "OTP_SKIPPED"
            notifications.append(sms_info)
        else:
            amount, txn_type = extract_amount_typed(body)
            if amount:
                merchant = extract_merchant(body)
                ref = extract_ref(body)
                desc = merchant if merchant else (body[:100]+"..." if len(body)>100 else body)
                cat = categorize(body, merchant)

                key = (sender, day_key, round(amount, 2))
                seen_keys[key].append({
                    "sender": sender, "amount": amount, "merchant": merchant,
                    "description": desc, "ref": ref, "date": ts, "dateStr": date_str_fmt,
                    "smsBody": body, "category": cat, "type": txn_type or "UNKNOWN",
                    "notificationCategory": notif_cat, "isBank": is_bank,
                })

                if txn_type is None:
                    sms_info["txnType"] = "LOW_CONFIDENCE"
                    sms_info["amount"] = amount
                    sms_info["category"] = cat
                    low_confidence.append(sms_info)
                    # Still add but mark as low confidence
                    raw_txns.append({"amount": amount, "type": "UNKNOWN", "merchant": merchant,
                        "description": desc, "sender": sender, "date": ts, "dateStr": date_str_fmt,
                        "smsBody": body, "ref": ref, "category": cat, "isBank": is_bank,
                        "notificationCategory": notif_cat, "confidence": "low"})
                else:
                    sms_info["txnType"] = txn_type
                    sms_info["amount"] = amount
                    sms_info["category"] = cat
                    raw_txns.append({"amount": amount, "type": txn_type, "merchant": merchant,
                        "description": desc, "sender": sender, "date": ts, "dateStr": date_str_fmt,
                        "smsBody": body, "ref": ref, "category": cat, "isBank": is_bank,
                        "notificationCategory": notif_cat, "confidence": "high"})
            else:
                sms_info["txnType"] = "NOTIFICATION"
                notifications.append(sms_info)

        # Reminder check
        reminder = analyze_reminder(body, sender, date_str)
        if reminder: reminders.append(reminder)

    # Dedup: mark duplicates (same sender + same day + same amount)
    dup_keys = {k for k,v in seen_keys.items() if len(v) > 1}
    for t in raw_txns:
        key = (t["sender"], datetime.fromtimestamp(t["date"]/1000).strftime("%Y-%m-%d"), round(t["amount"],2))
        if key in dup_keys:
            t["isDuplicate"] = True
            dup_info = seen_keys[key]
            t["duplicateCount"] = len(dup_info)
            t["duplicateIndex"] = [i for i, d in enumerate(dup_info) if d["date"] == t["date"]][0] if any(d["date"]==t["date"] for d in dup_info) else 0
            duplicates.append(t)
        else:
            t["isDuplicate"] = False

    # Final transaction lists
    income = [t for t in raw_txns if t["type"] == "INCOME" and not t.get("isDuplicate")]
    expenses = [t for t in raw_txns if t["type"] == "EXPENSE" and not t.get("isDuplicate")]
    # Also include duplicates in expense/income for totals but flagged
    income_all = [t for t in raw_txns if t["type"] == "INCOME"]
    expense_all = [t for t in raw_txns if t["type"] == "EXPENSE"]

    # Sort
    for lst in [income, expenses, income_all, expense_all, duplicates, reminders]:
        lst.sort(key=lambda x: x.get("date",0) or x.get("smsDate",0), reverse=True)

    # Group duplicates by amount-sender
    dup_groups = defaultdict(list)
    for t in duplicates:
        k = (t["sender"], round(t["amount"],2))
        dup_groups[k].append(t)

    # Monthly data
    monthly = defaultdict(lambda: {"income": 0, "expense": 0, "countIncome": 0, "countExpense": 0})
    for t in income_all:
        m = datetime.fromtimestamp(t["date"]/1000).strftime("%Y-%m")
        monthly[m]["income"] += t["amount"]
        monthly[m]["countIncome"] += 1
    for t in expense_all:
        m = datetime.fromtimestamp(t["date"]/1000).strftime("%Y-%m")
        monthly[m]["expense"] += t["amount"]
        monthly[m]["countExpense"] += 1

    all_months = sorted(monthly.keys())

    # Merchant totals (non-duplicate expenses)
    merchant_totals = defaultdict(float)
    for t in expenses:
        m = t["merchant"] if t["merchant"] else "Unknown"
        merchant_totals[m] += t["amount"]

    # Amount grouping for drill-down (same amount across all)
    amount_groups = defaultdict(list)
    for t in raw_txns:
        amt_key = round(t["amount"], 0)
        amount_groups[amt_key].append(t)

    # Sender stats
    sender_stats = defaultdict(lambda: {"income":0, "expense":0, "count":0, "isBank":False, "categories": set()})
    for t in raw_txns:
        s = t["sender"]
        sender_stats[s]["isBank"] = t["isBank"]
        sender_stats[s]["categories"].add(t["category"])
        if t["type"] in ("INCOME","EXPENSE"):
            sender_stats[s][t["type"].lower()] += t["amount"]
            sender_stats[s]["count"] += 1

    # Notification stats
    notif_counts = defaultdict(int)
    for n in notifications:
        notif_counts[n["notificationCategory"]] += 1

    expenses.sort(key=lambda x: x["date"], reverse=True)
    income.sort(key=lambda x: x["date"], reverse=True)

    result = {
        "totalSms": len(all_sms),
        "totalNotifications": len(notifications),
        "notificationCounts": dict(notif_counts),
        "totalTransactions": len(raw_txns),
        "totalIncome": round(sum(t["amount"] for t in income_all), 2),
        "totalExpense": round(sum(t["amount"] for t in expense_all), 2),
        "totalIncomeDeduped": round(sum(t["amount"] for t in income), 2),
        "totalExpenseDeduped": round(sum(t["amount"] for t in expenses), 2),
        "netBalance": round(sum(t["amount"] for t in income_all) - sum(t["amount"] for t in expense_all), 2),
        "incomeCount": len(income_all),
        "expenseCount": len(expense_all),
        "incomeCountDeduped": len(income),
        "expenseCountDeduped": len(expenses),
        "duplicateCount": len(duplicates),
        "duplicateGroups": [{"key": f"{s} - ₹{a:,.0f}", "sender": s, "amount": a, "count": len(v),
                             "items": sorted(v, key=lambda x: x["date"])}
                            for (s,a), v in sorted(dup_groups.items(), key=lambda x: sum(t["amount"] for t in x[1]), reverse=True)],
        "lowConfidenceCount": len(low_confidence),
        "reminderCount": len(reminders),
        "reminders": reminders,
        "income": income,
        "expenses": expenses,
        "incomeAll": income_all,
        "expenseAll": expense_all,
        "lowConfidence": low_confidence[:200],
        "notifications": notifications,
        "monthlyData": [{"month": m, "income": round(v["income"],2), "expense": round(v["expense"],2),
                         "countIncome": v["countIncome"], "countExpense": v["countExpense"]}
                        for m,v in sorted(monthly.items())],
        "topMerchants": sorted([{"name": n, "total": round(t,2)} for n,t in merchant_totals.items()], key=lambda x: x["total"], reverse=True)[:30],
        "topSenders": sorted([{"sender": s, "income": round(v["income"],2), "expense": round(v["expense"],2),
                               "count": v["count"], "isBank": v["isBank"],
                               "categories": list(v["categories"])} for s,v in sender_stats.items()],
                              key=lambda x: x["count"], reverse=True)[:30],
        "amountGroups": sorted([{"amount": a, "count": len(v),
                                  "total": round(sum(t["amount"] for t in v),2),
                                  "types": list(set(t["type"] for t in v))} for a,v in amount_groups.items()],
                                key=lambda x: x["count"], reverse=True)[:100],
    }
    return result


DASHBOARD_HTML = r"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>MyFinanceMate SMS Dashboard v2</title>
<style>
:root {
    --bg: #0f172a; --surface: #1e293b; --surface2: #334155;
    --text: #e2e8f0; --text2: #94a3b8; --accent: #6366f1;
    --green: #22c55e; --red: #ef4444; --yellow: #eab308; --blue: #3b82f6; --orange: #f97316;
    --border: #475569;
}
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); min-height: 100vh; }
.header { background: var(--surface); border-bottom: 1px solid var(--border); padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; position: sticky; top: 0; z-index: 100; }
.header h1 { font-size: 20px; font-weight: 600; }
.header .file-info { color: var(--text2); font-size: 13px; }
.summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; padding: 24px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 18px; }
.card .label { font-size: 11px; color: var(--text2); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }
.card .value { font-size: 24px; font-weight: 700; }
.card .value.income { color: var(--green); }
.card .value.expense { color: var(--red); }
.card .value.reminder { color: var(--yellow); }
.card .value.neutral { color: var(--blue); }
.card .value.orange { color: var(--orange); }
.card .sub { font-size: 11px; color: var(--text2); margin-top: 4px; }
.tabs { display: flex; gap: 0; padding: 0 24px; border-bottom: 1px solid var(--border); background: var(--surface); position: sticky; top: 57px; z-index: 99; overflow-x: auto; }
.tab { padding: 10px 18px; cursor: pointer; color: var(--text2); border-bottom: 2px solid transparent; font-size: 13px; font-weight: 500; white-space: nowrap; transition: all 0.2s; }
.tab:hover { color: var(--text); }
.tab.active { color: var(--accent); border-bottom-color: var(--accent); }
.tab-content { display: none; padding: 24px; }
.tab-content.active { display: block; }
.search-bar { margin-bottom: 16px; display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.search-bar input, .search-bar select { background: var(--surface2); border: 1px solid var(--border); color: var(--text); padding: 8px 14px; border-radius: 8px; font-size: 13px; outline: none; }
.search-bar input:focus, .search-bar select:focus { border-color: var(--accent); }
.search-bar input { flex: 1; min-width: 200px; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 8px 12px; text-align: left; font-size: 12px; border-bottom: 1px solid var(--border); }
th { background: var(--surface2); color: var(--text2); font-weight: 600; text-transform: uppercase; font-size: 10px; letter-spacing: 0.5px; position: sticky; z-index: 50; }
tr:hover td { background: rgba(99, 102, 241, 0.05); }
.amount { font-weight: 600; font-variant-numeric: tabular-nums; }
.amount.income { color: var(--green); }
.amount.expense { color: var(--red); }
.amount.reminder { color: var(--yellow); }
.amount.unknown { color: var(--orange); }
.badge { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 10px; font-weight: 600; }
.badge.income { background: rgba(34,197,94,0.15); color: var(--green); }
.badge.expense { background: rgba(239,68,68,0.15); color: var(--red); }
.badge.otp { background: rgba(249,115,22,0.15); color: var(--orange); }
.badge.monthly { background: rgba(59,130,246,0.15); color: var(--blue); }
.badge.none, .badge.other { background: rgba(148,163,184,0.15); color: var(--text2); }
.badge.bank { background: rgba(34,197,94,0.12); color: var(--green); border:1px solid var(--green); }
.badge.nonbank { background: rgba(148,163,184,0.12); color: var(--text2); border:1px solid var(--border); }
.badge.dup { background: rgba(234,179,8,0.15); color: var(--yellow); }
.badge.low { background: rgba(249,115,22,0.15); color: var(--orange); }
.badge.expiry { background: rgba(234,179,8,0.15); color: var(--yellow); }
.badge.recharge, .badge.delivery, .badge.appointment, .badge.promotion { background: rgba(148,163,184,0.12); color: var(--text2); }
.sms-body { max-width: 350px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text2); font-size: 11px; cursor: pointer; }
.sms-body:hover { white-space: normal; word-break: break-word; }
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }
@media(max-width:900px){ .chart-row { grid-template-columns: 1fr; } }
.chart-card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 18px; }
.chart-card h3 { font-size: 13px; margin-bottom: 14px; color: var(--text2); }
.bar-chart { display: flex; flex-direction: column; gap: 5px; }
.bar-row { display: flex; align-items: center; gap: 6px; }
.bar-label { width: 70px; font-size: 11px; color: var(--text2); text-align: right; flex-shrink: 0; }
.bar-container { flex:1; height: 18px; background: var(--surface2); border-radius: 3px; overflow: hidden; display:flex; }
.bar { height:100%; border-radius: 3px; min-width:1px; transition: width 0.5s; }
.bar.income { background: var(--green); }
.bar.expense { background: var(--red); }
.bar-value { font-size: 10px; color: var(--text2); min-width: 70px; text-align: right; }
.merchant-bar { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.merchant-name { width: 140px; font-size: 11px; color: var(--text2); text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex-shrink: 0; }
.merchant-bar-fill { flex:1; height: 14px; background: var(--surface2); border-radius: 3px; overflow: hidden; }
.merchant-bar-fill .bar { background: var(--accent); }
.merchant-val { font-size: 10px; color: var(--text2); min-width: 70px; }
.pagination { display: flex; align-items: center; gap: 10px; margin-top: 12px; justify-content: center; }
.pagination button { background: var(--surface2); border: 1px solid var(--border); color: var(--text); padding: 5px 12px; border-radius: 6px; cursor: pointer; font-size: 12px; }
.pagination button:hover { background: var(--accent); }
.pagination button:disabled { opacity: 0.4; cursor: default; }
.pagination button:disabled:hover { background: var(--surface2); }
.pagination span { color: var(--text2); font-size: 12px; }
.empty { text-align: center; padding: 40px; color: var(--text2); }

/* Monthly tab */
.month-selector { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.month-btn { background: var(--surface2); border: 1px solid var(--border); color: var(--text2); padding: 6px 14px; border-radius: 8px; cursor: pointer; font-size: 12px; transition: all 0.2s; }
.month-btn:hover { border-color: var(--accent); color: var(--text); }
.month-btn.active { background: var(--accent); border-color: var(--accent); color: #fff; }
.month-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin-bottom: 16px; }
.month-card { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 14px; }
.month-card .mc-label { font-size: 10px; color: var(--text2); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
.month-card .mc-value { font-size: 20px; font-weight: 700; }
.section-title { font-size: 14px; font-weight: 600; color: var(--text); margin: 18px 0 10px; padding-bottom: 6px; border-bottom: 1px solid var(--border); }
.month-detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 16px; }
@media(max-width:900px){ .month-detail-grid { grid-template-columns: 1fr; } }
.mini-table { max-height: 350px; overflow-y: auto; }
.mini-table table { font-size: 11px; }
.mini-table th { position: sticky; top: 0; z-index: 5; }
.month-nav { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.month-nav button { background: var(--surface2); border: 1px solid var(--border); color: var(--text); width: 30px; height: 30px; border-radius: 6px; cursor: pointer; font-size: 14px; display: flex; align-items: center; justify-content: center; }
.month-nav button:hover { background: var(--accent); }
.month-nav button:disabled { opacity: 0.3; cursor: default; }
.month-nav button:disabled:hover { background: var(--surface2); }
.month-nav .current-month { font-size: 15px; font-weight: 600; min-width: 110px; text-align: center; }
.day-breakdown { display: flex; flex-direction: column; gap: 3px; }
.day-bar-row { display: flex; align-items: center; gap: 4px; }
.day-label { width: 40px; font-size: 10px; color: var(--text2); text-align: right; flex-shrink: 0; }
.day-bars { flex:1; display: flex; gap: 2px; height: 12px; }
.day-bar { height:100%; border-radius: 2px; min-width:1px; }
.day-val { font-size: 9px; color: var(--text2); min-width: 100px; text-align: right; }

/* Drill-down card */
.drill-card { background: var(--surface); border: 1px solid var(--border); border-radius: 8px; padding: 12px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s; }
.drill-card:hover { border-color: var(--accent); }
.drill-card .dc-header { display: flex; justify-content: space-between; align-items: center; }
.drill-card .dc-amount { font-size: 14px; font-weight: 700; color: var(--accent); }
.drill-card .dc-count { font-size: 11px; color: var(--text2); }
.drill-card .dc-detail { display: none; margin-top: 10px; }
.drill-card.open .dc-detail { display: block; }
a { color: var(--accent); cursor: pointer; text-decoration: none; }
a:hover { text-decoration: underline; }
.sticky-sub { position: sticky; top: 105px; z-index: 45; }
</style>
</head>
<body>

<div class="header">
    <h1>MyFinanceMate SMS Dashboard v2</h1>
    <div class="file-info" id="fileInfo">Loading...</div>
</div>

<div class="summary" id="summary"></div>

<div class="tabs">
    <div class="tab active" data-tab="overview">Overview</div>
    <div class="tab" data-tab="monthly">Monthly</div>
    <div class="tab" data-tab="banksms">Bank SMS</div>
    <div class="tab" data-tab="income">Income</div>
    <div class="tab" data-tab="expenses">Expenses</div>
    <div class="tab" data-tab="duplicates">Duplicates</div>
    <div class="tab" data-tab="notifications">Notifications</div>
    <div class="tab" data-tab="reminders">Reminders</div>
    <div class="tab" data-tab="drilldown">Amount Drill-Down</div>
</div>

<div id="tab-overview" class="tab-content active"></div>
<div id="tab-monthly" class="tab-content"></div>
<div id="tab-banksms" class="tab-content"></div>
<div id="tab-income" class="tab-content"></div>
<div id="tab-expenses" class="tab-content"></div>
<div id="tab-duplicates" class="tab-content"></div>
<div id="tab-notifications" class="tab-content"></div>
<div id="tab-reminders" class="tab-content"></div>
<div id="tab-drilldown" class="tab-content"></div>

<script>
let DATA = null;
const PS = 50;
const st = { ip:0, ep:0, rp:0, bp:0, dp:0, np:0, lcp:0, sm:null, mi:0, me:0, mr:0 };

function fmt(n) { return new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:0}).format(n); }
function f2(n) { return new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:2}).format(n); }
function esc(s) { const d=document.createElement('div'); d.textContent=s; return d.innerHTML; }
function pg(items, page) { const s=page*PS; return {items:items.slice(s,s+PS),total:items.length,pages:Math.ceil(items.length/PS),page}; }

async function load() {
    const r = await fetch('/api/data');
    DATA = await r.json();
    document.getElementById('fileInfo').textContent = `${DATA.totalSms.toLocaleString()} SMS | ${DATA.totalTransactions} txns | ${DATA.duplicateCount} dup | ${DATA.reminderCount} reminders | ${DATA.lowConfidenceCount} low-conf`;
    renderSummary(); renderOverview(); renderMonthly(); renderBankSms(); renderIncome(); renderExpenses(); renderDuplicates(); renderNotifications(); renderReminders(); renderDrilldown();
}

function renderSummary() {
    document.getElementById('summary').innerHTML = `
        <div class="card"><div class="label">SMS</div><div class="value neutral">${DATA.totalSms.toLocaleString()}</div></div>
        <div class="card"><div class="label">Income</div><div class="value income">${fmt(DATA.totalIncome)}</div><div class="sub">${DATA.incomeCount} txns <span class="badge" style="background:rgba(234,179,8,0.15);color:var(--yellow)">${DATA.incomeCountDeduped} deduped</span></div></div>
        <div class="card"><div class="label">Expenses</div><div class="value expense">${fmt(DATA.totalExpense)}</div><div class="sub">${DATA.expenseCount} txns <span class="badge" style="background:rgba(234,179,8,0.15);color:var(--yellow)">${DATA.expenseCountDeduped} deduped</span></div></div>
        <div class="card"><div class="label">Net</div><div class="value ${DATA.netBalance>=0?'income':'expense'}">${fmt(DATA.netBalance)}</div></div>
        <div class="card"><div class="label">Duplicates</div><div class="value orange">${DATA.duplicateCount}</div></div>
        <div class="card"><div class="label">Reminders</div><div class="value reminder">${DATA.reminderCount}</div><div class="sub">Low-conf: ${DATA.lowConfidenceCount}</div></div>
    `;
}

function renderOverview() {
    const md = DATA.monthlyData;
    const mx = Math.max(...md.map(m=>Math.max(m.income,m.expense)),1);
    const bar = md.map(m=>`<div class="bar-row"><div class="bar-label">${m.month}</div><div class="bar-container"><div class="bar income" style="width:${(m.income/mx*100).toFixed(1)}%" title="I:${fmt(m.income)}"></div></div><div class="bar-container"><div class="bar expense" style="width:${(m.expense/mx*100).toFixed(1)}%" title="E:${fmt(m.expense)}"></div></div><div class="bar-value">${fmt(m.income)} / ${fmt(m.expense)}</div></div>`).join('');

    const maxM = DATA.topMerchants.length?DATA.topMerchants[0].total:1;
    const mb = DATA.topMerchants.slice(0,15).map(m=>`<div class="merchant-bar"><div class="merchant-name" title="${esc(m.name)}">${esc(m.name)}</div><div class="merchant-bar-fill"><div class="bar" style="width:${(m.total/maxM*100).toFixed(1)}%"></div></div><div class="merchant-val">${fmt(m.total)}</div></div>`).join('');

    const sr = DATA.topSenders.slice(0,15).map(s=>`<tr><td>${esc(s.sender)} <span class="badge ${s.isBank?'bank':'nonbank'}">${s.isBank?'bank':'non'}</span></td><td class="amount income">${fmt(s.income)}</td><td class="amount expense">${fmt(s.expense)}</td><td>${s.count}</td></tr>`).join('');

    const nc = DATA.notificationCounts||{};
    const ncHtml = Object.entries(nc).map(([k,v])=>`<tr><td><span class="badge ${k.toLowerCase()}">${k}</span></td><td>${v}</td></tr>`).join('');

    document.getElementById('tab-overview').innerHTML = `
        <div class="chart-row">
            <div class="chart-card"><h3>Monthly Income vs Expense</h3><div class="bar-chart">${bar}</div></div>
            <div class="chart-card"><h3>Top Expense Merchants</h3>${mb||'<div class="empty">No data</div>'}</div>
        </div>
        <div class="chart-row">
            <div class="chart-card"><h3>Top Senders</h3><div class="mini-table"><table><thead><tr><th>Sender</th><th>Income</th><th>Expense</th><th>#</th></tr></thead><tbody>${sr}</tbody></table></div></div>
            <div class="chart-card"><h3>Notifications by Type</h3><table><thead><tr><th>Category</th><th>Count</th></tr></thead><tbody>${ncHtml||'<tr><td colspan="2" class="empty">None</td></tr>'}</tbody></table></div>
        </div>
        <div class="chart-row">
            <div class="chart-card"><h3>Summary by Month</h3><div class="mini-table"><table><thead><tr><th>Month</th><th>Income</th><th>Expense</th><th>Net</th><th>#I</th><th>#E</th></tr></thead><tbody>
            ${md.map(m=>`<tr><td>${m.month}</td><td class="amount income">${fmt(m.income)}</td><td class="amount expense">${fmt(m.expense)}</td><td class="amount ${m.income-m.expense>=0?'income':'expense'}">${fmt(m.income-m.expense)}</td><td>${m.countIncome}</td><td>${m.countExpense}</td></tr>`).join('')}
            </tbody></table></div></div>
            <div class="chart-card"><h3>Data Quality</h3>
            <table><thead><tr><th>Metric</th><th>Value</th></tr></thead><tbody>
            <tr><td>Total SMS</td><td>${DATA.totalSms.toLocaleString()}</td></tr>
            <tr><td>Bank transactions</td><td>${DATA.totalTransactions}</td></tr>
            <tr><td>Duplicates flagged</td><td>${DATA.duplicateCount} <span class="badge dup">review</span></td></tr>
            <tr><td>Low confidence (untyped)</td><td>${DATA.lowConfidenceCount} <span class="badge low">needs review</span></td></tr>
            <tr><td>Notifications (non-financial)</td><td>${DATA.totalNotifications}</td></tr>
            </tbody></table></div>
        </div>
    `;
}

/* Monthly */
function gm(ym){const[y,m]=ym.split('-'),n=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];return n[+m-1]+' '+y;}
function gmt(month,type){const all=type==='INCOME'?DATA.income:DATA.expenses;return all.filter(t=>{const d=new Date(t.date),ym=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');return ym===month;});}
function gmr(month){return DATA.reminders.filter(r=>{const d=new Date(r.smsDate),ym=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');return ym===month;});}

function selMonth(m){st.sm=m;st.mi=st.me=st.mr=0;renderMonthlyDetail();}
function navM(d){const ms=DATA.monthlyData.map(x=>x.month),i=ms.indexOf(st.sm),ni=i+d;if(ni>=0&&ni<ms.length)selMonth(ms[ni]);}

function renderMonthly(){
    const ms=DATA.monthlyData.map(x=>x.month);
    if(!st.sm||!ms.includes(st.sm))st.sm=ms[ms.length-1]||ms[0];
    document.getElementById('tab-monthly').innerHTML=`<div class="month-selector">${ms.map(m=>`<button class="month-btn ${m===st.sm?'active':''}" onclick="selMonth('${m}')">${gm(m)}</button>`).join('')}</div><div id="monthly-detail"></div>`;
    renderMonthlyDetail();
}

function renderMonthlyDetail(){
    const m=st.sm;if(!m)return;
    const ms=DATA.monthlyData.map(x=>x.month),idx=ms.indexOf(m);
    const md=DATA.monthlyData.find(x=>x.month===m)||{income:0,expense:0,countIncome:0,countExpense:0};
    const mi=gmt(m,'INCOME'),me=gmt(m,'EXPENSE'),mr=gmr(m);
    const net=md.income-md.expense,tr=mr.reduce((s,r)=>s+(r.amount||0),0);
    document.querySelectorAll('.month-btn').forEach(b=>b.classList.toggle('active',b.textContent===gm(m)));

    const mt={};
    me.forEach(t=>{const n=t.merchant||'Unknown';mt[n]=(mt[n]||0)+t.amount;});
    const topM=Object.entries(mt).sort((a,b)=>b[1]-a[1]).slice(0,10),mxM=topM.length?topM[0][1]:1;

    const dd={};
    mi.forEach(t=>{const d=new Date(t.date).getDate();if(!dd[d])dd[d]={i:0,e:0};dd[d].i+=t.amount;});
    me.forEach(t=>{const d=new Date(t.date).getDate();if(!dd[d])dd[d]={i:0,e:0};dd[d].e+=t.amount;});
    const days=Object.keys(dd).map(Number).sort((a,b)=>a-b),mxD=Math.max(...days.map(d=>Math.max(dd[d].i,dd[d].e)),1);

    const pi=pg(mi,st.mi),pe=pg(me,st.me),pr=pg(mr,st.mr);

    const sb={};
    [...mi,...me].forEach(t=>{if(!sb[t.sender])sb[t.sender]={i:0,e:0,c:0};sb[t.sender][t.type.toLowerCase()]+=t.amount;sb[t.sender].c++;});
    const ts=Object.entries(sb).sort((a,b)=>b[1].c-a[1].c).slice(0,10);

    document.getElementById('monthly-detail').innerHTML=`
        <div class="month-nav">
            <button ${idx<=0?'disabled':''} onclick="navM(-1)">&#9664;</button>
            <div class="current-month">${gm(m)}</div>
            <button ${idx>=ms.length-1?'disabled':''} onclick="navM(1)">&#9654;</button>
        </div>
        <div class="month-summary">
            <div class="month-card"><div class="mc-label">Income</div><div class="mc-value income">${fmt(md.income)}</div><div class="sub">${md.countIncome} txns</div></div>
            <div class="month-card"><div class="mc-label">Expenses</div><div class="mc-value expense">${fmt(md.expense)}</div><div class="sub">${md.countExpense} txns</div></div>
            <div class="month-card"><div class="mc-label">Net</div><div class="mc-value ${net>=0?'income':'expense'}">${fmt(net)}</div></div>
            <div class="month-card"><div class="mc-label">Reminders</div><div class="mc-value reminder">${mr.length}</div><div class="sub">${fmt(tr)}</div></div>
            <div class="month-card"><div class="mc-label">Avg Daily</div><div class="mc-value expense">${fmt(days.length?md.expense/days.length:0)}</div><div class="sub">${days.length}d</div></div>
            <div class="month-card"><div class="mc-label">Max Expense</div><div class="mc-value expense">${me.length?fmt(Math.max(...me.map(t=>t.amount))):'-'}</div></div>
        </div>

        <div class="section-title">Daily Breakdown</div>
        <div class="chart-card" style="margin-bottom:16px"><div class="day-breakdown">${days.map(d=>`<div class="day-bar-row"><div class="day-label">${String(d).padStart(2,'0')}</div><div class="day-bars"><div class="day-bar income" style="width:${(dd[d].i/mxD*100).toFixed(1)}%;background:var(--green)"></div><div class="day-bar expense" style="width:${(dd[d].e/mxD*100).toFixed(1)}%;background:var(--red)"></div></div><div class="day-val">+${fmt(dd[d].i)} / -${fmt(dd[d].e)}</div></div>`).join('')}</div></div>

        <div class="month-detail-grid">
            <div class="chart-card"><h3>Top Merchants</h3>${topM.map(([n,t])=>`<div class="merchant-bar"><div class="merchant-name" title="${esc(n)}">${esc(n)}</div><div class="merchant-bar-fill"><div class="bar" style="width:${(t/mxM*100).toFixed(1)}%"></div></div><div class="merchant-val">${fmt(t)}</div></div>`).join('')||'<div class="empty">None</div>'}</div>
            <div class="chart-card"><h3>Top Senders</h3><table><thead><tr><th>Sender</th><th>Income</th><th>Expense</th><th>#</th></tr></thead><tbody>${ts.map(([s,d])=>`<tr><td style="font-size:11px">${esc(s)}</td><td class="amount income">${fmt(d.i)}</td><td class="amount expense">${fmt(d.e)}</td><td>${d.c}</td></tr>`).join('')}</tbody></table></div>
        </div>

        <div class="section-title">Income (${mi.length})</div>
        <div class="mini-table"><table><thead><tr><th>Date</th><th>Amount</th><th>Category</th><th>Merchant</th><th>Sender</th></tr></thead><tbody>${pi.items.map(t=>`<tr><td>${t.dateStr}</td><td class="amount income">${f2(t.amount)}</td><td><span class="badge">${esc(t.category)}</span></td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)}</td></tr>`).join('')||'<tr><td colspan="5" class="empty">None</td></tr>'}</tbody></table></div>
        ${mhp('mi',pi)}
        <div class="section-title">Expenses (${me.length})</div>
        <div class="mini-table"><table><thead><tr><th>Date</th><th>Amount</th><th>Category</th><th>Merchant</th><th>Sender</th></tr></thead><tbody>${pe.items.map(t=>`<tr><td>${t.dateStr}</td><td class="amount expense">${f2(t.amount)}</td><td><span class="badge">${esc(t.category)}</span></td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)}</td></tr>`).join('')||'<tr><td colspan="5" class="empty">None</td></tr>'}</tbody></table></div>
        ${mhp('me',pe)}
        <div class="section-title">Reminders (${mr.length})</div>
        <div class="mini-table"><table><thead><tr><th>Title</th><th>Amount</th><th>Trigger</th><th>Sender</th></tr></thead><tbody>${pr.items.map(r=>`<tr><td>${esc(r.title)}</td><td class="amount reminder">${r.amount?f2(r.amount):'-'}</td><td>${r.triggerDateStr}</td><td>${esc(r.sender)}</td></tr>`).join('')||'<tr><td colspan="4" class="empty">None</td></tr>'}</tbody></table></div>
        ${mhp('mr',pr)}
    `;
}
function mhp(k,p){if(p.pages<=1)return '';return `<div class="pagination"><button ${p.page===0?'disabled':''} onclick="cmp('${k}',-1)">Prev</button><span>Page ${p.page+1}/${p.pages} (${p.total})</span><button ${p.page>=p.pages-1?'disabled':''} onclick="cmp('${k}',1)">Next</button></div>`;}
function cmp(k,d){const map={mi:'mi',me:'me',mr:'mr'};st[map[k]]+=d;renderMonthlyDetail();}

/* Bank SMS */
function renderBankSms(){
    const allBank = [...DATA.income, ...DATA.expenses].filter(t=>t.isBank).sort((a,b)=>b.date-a.date);
    const allNonBank = [...DATA.income, ...DATA.expenses].filter(t=>!t.isBank).sort((a,b)=>b.date-a.date);
    const p = pg(allBank, st.bp);
    const rows = p.items.map(t=>`<tr><td>${t.dateStr}</td><td><span class="badge ${t.type.toLowerCase()}">${t.type}</span></td><td class="amount ${t.type.toLowerCase()}">${f2(t.amount)}</td><td><span class="badge">${esc(t.category)}</span></td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)} <span class="badge bank">bank</span></td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('');
    const nonBankRows = allNonBank.map(t=>`<tr><td>${t.dateStr}</td><td><span class="badge ${t.type.toLowerCase()}">${t.type}</span></td><td class="amount ${t.type.toLowerCase()}">${f2(t.amount)}</td><td>${esc(t.sender)} <span class="badge nonbank">non-bank</span></td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('');
    document.getElementById('tab-banksms').innerHTML = `
        <div class="month-summary" style="margin-bottom:16px">
            <div class="month-card"><div class="mc-label">Bank SMS Txns</div><div class="mc-value income">${allBank.length}</div><div class="sub">${fmt(allBank.reduce((s,t)=>s+t.amount,0))}</div></div>
            <div class="month-card"><div class="mc-label">Non-Bank Smatches</div><div class="mc-value orange">${allNonBank.length}</div><div class="sub">${fmt(allNonBank.reduce((s,t)=>s+t.amount,0))} <span class="badge low">review</span></div></div>
        </div>
        <div class="section-title">Verified Bank Transactions (${allBank.length})</div>
        <div class="search-bar"><input type="text" placeholder="Search bank SMS..." oninput="sbSearch(this.value)"></div>
        <table><thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Category</th><th>Merchant</th><th>Sender</th><th>SMS</th></tr></thead><tbody id="bank-tbody">${rows||'<tr><td colspan="7" class="empty">None</td></tr>'}</tbody></table>
        <div class="pagination"><button ${st.bp===0?'disabled':''} onclick="chg('bp',-1)">Prev</button><span>Page ${st.bp+1} of ${Math.ceil(allBank.length/PS)}</span><button ${st.bp>=Math.ceil(allBank.length/PS)-1?'disabled':''} onclick="chg('bp',1)">Next</button></div>

        <div class="section-title">Non-Bank (unverified) Transactions (${allNonBank.length})</div>
        <div class="mini-table"><table><thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Sender</th><th>SMS</th></tr></thead><tbody>${nonBankRows}</tbody></table></div>
    `;
}
function sbSearch(q){
    const allBank = [...DATA.income, ...DATA.expenses].filter(t=>t.isBank&&(t.smsBody.toLowerCase().includes(q.toLowerCase())||t.sender.toLowerCase().includes(q.toLowerCase())||(t.merchant||'').toLowerCase().includes(q.toLowerCase())));
    const p = pg(allBank, 0); const tbody=document.getElementById('bank-tbody');
    tbody.innerHTML=p.items.map(t=>`<tr><td>${t.dateStr}</td><td><span class="badge ${t.type.toLowerCase()}">${t.type}</span></td><td class="amount ${t.type.toLowerCase()}">${f2(t.amount)}</td><td><span class="badge">${esc(t.category)}</span></td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)}</td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('')||'<tr><td colspan="7" class="empty">No results</td></tr>';
}

function chg(k,d){st[k]+=d;const r={bp:renderBankSms,ip:renderIncome,ep:renderExpenses,rp:renderReminders,dp:renderDuplicates,np:renderNotifications};r[k]();}

/* Income */
function renderIncome(){
    const p=pg(DATA.income,st.ip);
    document.getElementById('tab-income').innerHTML=`<div class="search-bar"><input type="text" placeholder="Search..." oninput="si(this.value)"></div><table><thead><tr><th>Date</th><th>Amount</th><th>Category</th><th>Merchant</th><th>Sender</th><th>SMS</th></tr></thead><tbody>${p.items.map(t=>`<tr><td>${t.dateStr}</td><td class="amount income">${f2(t.amount)}</td><td><span class="badge">${esc(t.category)}</span></td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)}</td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('')||'<tr><td colspan="6" class="empty">None</td></tr>'}</tbody></table>${ph('ip',p)}`;
}
function si(q){const f=DATA.income.filter(t=>(t.smsBody+' '+t.sender+' '+(t.merchant||'')).toLowerCase().includes(q.toLowerCase()));const p=pg(f,0);document.querySelector('#tab-income tbody').innerHTML=p.items.map(t=>`<tr><td>${t.dateStr}</td><td class="amount income">${f2(t.amount)}</td><td><span class="badge">${esc(t.category)}</span></td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)}</td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('')||'<tr><td colspan="6" class="empty">No results</td></tr>';}

/* Expenses */
function renderExpenses(){
    const p=pg(DATA.expenses,st.ep);
    document.getElementById('tab-expenses').innerHTML=`<div class="search-bar"><input type="text" placeholder="Search..." oninput="se(this.value)"></div><table><thead><tr><th>Date</th><th>Amount</th><th>Category</th><th>Merchant</th><th>Sender</th><th>SMS</th></tr></thead><tbody>${p.items.map(t=>`<tr><td>${t.dateStr}</td><td class="amount expense">${f2(t.amount)}</td><td><span class="badge">${esc(t.category)}</span></td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)}</td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('')||'<tr><td colspan="6" class="empty">None</td></tr>'}</tbody></table>${ph('ep',p)}`;
}
function se(q){const f=DATA.expenses.filter(t=>(t.smsBody+' '+t.sender+' '+(t.merchant||'')).toLowerCase().includes(q.toLowerCase()));const p=pg(f,0);document.querySelector('#tab-expenses tbody').innerHTML=p.items.map(t=>`<tr><td>${t.dateStr}</td><td class="amount expense">${f2(t.amount)}</td><td><span class="badge">${esc(t.category)}</span></td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)}</td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('')||'<tr><td colspan="6" class="empty">No results</td></tr>';}

/* Duplicates */
function renderDuplicates(){
    const dg = DATA.duplicateGroups||[];
    let html = `<div class="month-summary" style="margin-bottom:16px">
        <div class="month-card"><div class="mc-label">Duplicate Groups</div><div class="mc-value orange">${dg.length}</div></div>
        <div class="month-card"><div class="mc-label">Total Duplicate Txns</div><div class="mc-value orange">${DATA.duplicateCount}</div></div>
    </div>`;
    dg.forEach(g => {
        const collapsed = g.items.length > 3;
        const visible = collapsed ? g.items.slice(0,3) : g.items;
        const hidden = collapsed ? g.items.slice(3) : [];
        html += `<div class="drill-card" onclick="this.classList.toggle('open')">
            <div class="dc-header">
                <div><span class="dc-amount">${f2(g.amount)}</span> × ${g.count}x <span class="badge">${esc(g.sender)}</span></div>
                <div class="dc-count">${collapsed ? 'Click to expand ▼' : '▼'}</div>
            </div>
            <div class="dc-detail">
                <table><thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Merchant</th><th>SMS</th></tr></thead><tbody>
                ${g.items.map(t=>`<tr><td>${t.dateStr}</td><td><span class="badge ${t.type.toLowerCase()}">${t.type}</span></td><td class="amount ${t.type.toLowerCase()}">${f2(t.amount)}</td><td>${esc(t.merchant||'-')}</td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('')}
                </tbody></table>
            </div>
        </div>`;
    });
    document.getElementById('tab-duplicates').innerHTML = html || '<div class="empty">No duplicates found</div>';
}

/* Notifications */
function renderNotifications(){
    const notifs = DATA.notifications||[];
    const cats = {};
    notifs.forEach(n=>{const c=n.notificationCategory;if(!cats[c])cats[c]=[];cats[c].push(n);});
    let html = `<div class="month-summary" style="margin-bottom:16px">${Object.entries(cats).map(([k,v])=>`<div class="month-card"><div class="mc-label"><span class="badge ${k.toLowerCase()}">${k}</span></div><div class="mc-value neutral">${v.length}</div></div>`).join('')}</div>`;
    Object.entries(cats).forEach(([cat, items]) => {
        const p = pg(items, st.np);
        html += `<div class="section-title">${cat} (${items.length})</div><div class="mini-table"><table><thead><tr><th>Date</th><th>Sender</th><th>SMS</th></tr></thead><tbody>${p.items.map(n=>`<tr><td>${n.dateStr}</td><td>${esc(n.sender)}</td><td class="sms-body" title="${esc(n.body)}">${esc(n.body)}</td></tr>`).join('')||'<tr><td colspan="3" class="empty">None</td></tr>'}</tbody></table></div>`;
    });
    document.getElementById('tab-notifications').innerHTML = html;
}

/* Reminders */
function renderReminders(){
    const p=pg(DATA.reminders,st.rp);
    document.getElementById('tab-reminders').innerHTML=`<div class="search-bar"><input type="text" placeholder="Search..." oninput="sr(this.value)"></div><table><thead><tr><th>Title</th><th>Amount</th><th>Recurrence</th><th>Trigger</th><th>SMS Date</th><th>Sender</th><th>Keyword</th><th>SMS</th></tr></thead><tbody>${p.items.map(r=>`<tr><td>${esc(r.title)}</td><td class="amount reminder">${r.amount?f2(r.amount):'-'}</td><td><span class="badge ${r.recurrence.toLowerCase()}">${r.recurrence}</span></td><td>${r.triggerDateStr}</td><td>${r.smsDateStr}</td><td>${esc(r.sender)}</td><td><span class="badge">${esc(r.matchedKeyword)}</span></td><td class="sms-body" title="${esc(r.smsBody)}">${esc(r.smsBody)}</td></tr>`).join('')||'<tr><td colspan="8" class="empty">None</td></tr>'}</tbody></table>${ph('rp',p)}`;
}
function sr(q){const f=DATA.reminders.filter(r=>(r.smsBody+' '+r.sender+' '+r.title).toLowerCase().includes(q.toLowerCase()));const p=pg(f,0);document.querySelector('#tab-reminders tbody').innerHTML=p.items.map(r=>`<tr><td>${esc(r.title)}</td><td class="amount reminder">${r.amount?f2(r.amount):'-'}</td><td><span class="badge ${r.recurrence.toLowerCase()}">${r.recurrence}</span></td><td>${r.triggerDateStr}</td><td>${r.smsDateStr}</td><td>${esc(r.sender)}</td><td><span class="badge">${esc(r.matchedKeyword)}</span></td><td class="sms-body" title="${esc(r.smsBody)}">${esc(r.smsBody)}</td></tr>`).join('')||'<tr><td colspan="8" class="empty">No results</td></tr>';}

/* Amount Drill-down */
function renderDrilldown(){
    const ag = DATA.amountGroups||[];
    let html = `<div class="search-bar"><input type="text" placeholder="Filter by amount..." oninput="sa(this.value)"></div>
    <div id="drill-list">${ag.map(g=>`<div class="drill-card" onclick="this.classList.toggle('open')">
        <div class="dc-header"><div><span class="dc-amount">${f2(g.amount)}</span> <span class="dc-count">${g.count} txns | ${g.types.join(', ')}</span></div><div class="dc-count">▼</div></div>
        <div class="dc-detail"><div id="drill-${g.amount}" class="mini-table"><table><thead><tr><th>Date</th><th>Type</th><th>Amount</th><th>Merchant</th><th>Sender</th><th>Category</th><th>SMS</th></tr></thead><tbody>
            <tr><td colspan="7"><div class="empty">Loading...</div></td></tr>
        </tbody></table></div></div></div>`).join('')}</div>`;
    document.getElementById('tab-drilldown').innerHTML = html;
    // Load drill-down details async
    ag.forEach(g => {
        const div = document.querySelector(`#drill-${g.amount} tbody`);
        if (!div) return;
        // get matching items from all data
        const all = [...(DATA.incomeAll||DATA.income), ...(DATA.expenseAll||DATA.expenses)].filter(t => Math.round(t.amount) === Math.round(g.amount));
        div.innerHTML = all.map(t => `<tr><td>${t.dateStr}</td><td><span class="badge ${t.type===null?'unknown':t.type.toLowerCase()}">${t.type||'?'}</span></td><td class="amount ${t.type===null?'unknown':t.type.toLowerCase()}">${f2(t.amount)}</td><td>${esc(t.merchant||'-')}</td><td>${esc(t.sender)}</td><td><span class="badge">${esc(t.category)||'-'}</span></td><td class="sms-body" title="${esc(t.smsBody)}">${esc(t.smsBody)}</td></tr>`).join('') || '<tr><td colspan="7" class="empty">None</td></tr>';
    });
}
function sa(q){
    const all = DATA.amountGroups||[];
    const f = all.filter(g => g.amount.toString().includes(q) || g.amount.toFixed(0).includes(q));
    document.getElementById('drill-list').innerHTML = f.map(g => `<div class="drill-card" onclick="this.classList.toggle('open')">
        <div class="dc-header"><div><span class="dc-amount">${f2(g.amount)}</span> <span class="dc-count">${g.count} txns | ${g.types.join(', ')}</span></div><div class="dc-count">▼</div></div>
        <div class="dc-detail">Loading...</div></div>`
    ).join('') || '<div class="empty">No matches</div>';
}

function ph(k,p){return `<div class="pagination"><button ${p.page===0?'disabled':''} onclick="chg('${k}',-1)">Prev</button><span>Page ${p.page+1}/${p.pages} (${p.total})</span><button ${p.page>=p.pages-1?'disabled':''} onclick="chg('${k}',1)">Next</button></div>`;}

document.querySelectorAll('.tab').forEach(t=>{t.addEventListener('click',()=>{document.querySelectorAll('.tab').forEach(x=>x.classList.remove('active'));document.querySelectorAll('.tab-content').forEach(x=>x.classList.remove('active'));t.classList.add('active');document.getElementById('tab-'+t.dataset.tab).classList.add('active');});});

load();
</script>
</body>
</html>"""


class DashHandler(SimpleHTTPRequestHandler):
    cache = None
    def do_GET(self):
        p = urlparse(self.path)
        if p.path in ('/','/index.html'):
            self.send_response(200)
            self.send_header('Content-Type','text/html; charset=utf-8')
            self.end_headers()
            self.wfile.write(DASHBOARD_HTML.encode('utf-8'))
        elif p.path == '/api/data':
            if DashHandler.cache is None:
                xml_path = os.environ.get('SMS_XML_PATH','')
                if not xml_path:
                    d = os.path.dirname(os.path.abspath(__file__))
                    xml_path = os.path.join(os.path.dirname(d), 'sms_backup_20260717_111345.xml')
                DashHandler.cache = parse_xml_file(xml_path)
            self.send_response(200)
            self.send_header('Content-Type','application/json; charset=utf-8')
            self.send_header('Access-Control-Allow-Origin','*')
            self.end_headers()
            self.wfile.write(json.dumps(DashHandler.cache).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()
    def log_message(self, f, *a):
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {a[0]}")

def main():
    port = int(sys.argv[1]) if len(sys.argv)>1 else 8765
    p2 = sys.argv[2] if len(sys.argv)>2 else None
    if p2: os.environ['SMS_XML_PATH'] = p2
    s = HTTPServer(('127.0.0.1', port), DashHandler)
    print(f"SMS Dashboard v2 running at http://127.0.0.1:{port}")
    try: s.serve_forever()
    except KeyboardInterrupt: s.server_close()

if __name__ == '__main__':
    main()
