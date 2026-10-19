#!/usr/bin/env python3
"""Rename the mod id inside .nbt structure files.

usage:  python fix_nbt.py OLD_ID NEW_ID FOLDER [--apply]
Only .nbt files are touched. Without --apply nothing is written.
"""
import argparse
import pathlib

import nbtlib
from nbtlib import tag


def fix(node, old, new):
    """Replaces 'old:' by 'new:' in every string value and every compound key.

    Returns (changes made, strings that still contain the old id without a colon).
    """
    old_p, new_p = old + ":", new + ":"
    changes = leftovers = 0

    if isinstance(node, dict):
        for key in list(node.keys()):
            value = node.pop(key)
            if old_p in key:
                key = key.replace(old_p, new_p)
                changes += 1
            if old in key:
                leftovers += 1
            if isinstance(value, tag.String):
                text = str(value)
                if old_p in text:
                    value = tag.String(text.replace(old_p, new_p))
                    changes += 1
                if old in str(value):
                    leftovers += 1
            else:
                c, l = fix(value, old, new)
                changes += c
                leftovers += l
            node[key] = value

    elif isinstance(node, list):
        for i, value in enumerate(node):
            if isinstance(value, tag.String):
                text = str(value)
                if old_p in text:
                    node[i] = tag.String(text.replace(old_p, new_p))
                    changes += 1
                if old in str(node[i]):
                    leftovers += 1
            else:
                c, l = fix(value, old, new)
                changes += c
                leftovers += l

    return changes, leftovers


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("old_id")
    ap.add_argument("new_id")
    ap.add_argument("folder")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    files = sorted(pathlib.Path(args.folder).rglob("*.nbt"))
    mode = "APPLYING" if args.apply else "DRY RUN (add --apply to write the files)"
    print(f"{mode} - {len(files)} .nbt file(s) found")

    total = 0
    for path in files:
        data = nbtlib.load(path)
        changes, leftovers = fix(data, args.old_id, args.new_id)
        if changes or leftovers:
            note = f", {leftovers} string(s) still mention '{args.old_id}' without a colon" if leftovers else ""
            print(f"  {changes:5d} change(s)  {path}{note}")
        if changes and args.apply:
            data.save()
        total += changes
    print(f"done: {total} change(s) in total")


if __name__ == "__main__":
    main()