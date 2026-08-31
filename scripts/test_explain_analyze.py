#!/usr/bin/env python3
import importlib.util
import os
import unittest

MODULE_PATH = os.path.join(os.path.dirname(__file__), "explain_analyze.py")
spec = importlib.util.spec_from_file_location("explain_analyze", MODULE_PATH)
ea = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ea)


class TestFindSeqScans(unittest.TestCase):
    def test_no_seq_scan_returns_empty(self):
        output = (
            "Index Scan using idx_transactions_account_id on transactions "
            "(cost=0.29..8.31 rows=1 width=64) (actual time=0.020..0.021 rows=1 loops=1)"
        )
        self.assertEqual(ea.find_seq_scans(output), [])

    def test_single_seq_scan_is_found(self):
        output = "Seq Scan on transactions  (cost=0.00..18.10 rows=810 width=64)"
        result = ea.find_seq_scans(output)
        self.assertEqual(len(result), 1)
        self.assertIn("Seq Scan on transactions", result[0])

    def test_multiple_seq_scans_are_all_found(self):
        output = "\n".join([
            "Seq Scan on transactions  (cost=0.00..18.10 rows=810 width=64)",
            "  ->  Seq Scan on accounts  (cost=0.00..1.05 rows=5 width=8)",
            "Planning Time: 0.123 ms",
        ])
        self.assertEqual(len(ea.find_seq_scans(output)), 2)


if __name__ == "__main__":
    unittest.main()
