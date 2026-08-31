#!/usr/bin/env python3
import importlib.util
import os
import sys
import unittest
from datetime import date
from io import StringIO

MODULE_PATH = os.path.join(os.path.dirname(__file__), "generate_mock_transactions.py")
spec = importlib.util.spec_from_file_location("generate_mock_transactions", MODULE_PATH)
gmt = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gmt)


def col(name, data_type="text", nullable=True, default=None):
    return {"name": name, "type": data_type, "nullable": nullable, "default": default}


class TestSqlLiteral(unittest.TestCase):
    def test_none_becomes_null(self):
        self.assertEqual(gmt.sql_literal(None), "NULL")

    def test_string_is_quoted_and_escaped(self):
        self.assertEqual(gmt.sql_literal("O'Brien"), "'O''Brien'")

    def test_number_is_not_quoted(self):
        self.assertEqual(gmt.sql_literal(42), "42")
        self.assertEqual(gmt.sql_literal(19.99), "19.99")


class TestGenerateValue(unittest.TestCase):
    def setUp(self):
        self.start = date(2024, 1, 1)
        self.end = date(2024, 12, 31)

    def test_account_id_uses_provided_value(self):
        value = gmt.generate_value(col("account_id"), 7, None, None, self.start, self.end)
        self.assertEqual(value, 7)

    def test_amount_is_within_realistic_range(self):
        value = gmt.generate_value(col("amount"), 1, None, None, self.start, self.end)
        self.assertGreaterEqual(value, 1.00)
        self.assertLessEqual(value, 500.00)

    def test_date_is_within_requested_range(self):
        value = gmt.generate_value(col("date"), 1, None, None, self.start, self.end)
        parsed = date.fromisoformat(value)
        self.assertGreaterEqual(parsed, self.start)
        self.assertLessEqual(parsed, self.end)

    def test_type_is_credit_or_debit(self):
        value = gmt.generate_value(col("type"), 1, None, None, self.start, self.end)
        self.assertIn(value, ("CREDIT", "DEBIT"))

    def test_description_is_from_the_word_list(self):
        value = gmt.generate_value(col("description"), 1, None, None, self.start, self.end)
        self.assertIn(value, gmt.DESCRIPTIONS)

    def test_nullable_unrecognized_column_returns_none(self):
        value = gmt.generate_value(col("some_new_column", nullable=True), 1, None, None, self.start, self.end)
        self.assertIsNone(value)

    def test_not_null_column_with_default_omits_value(self):
        value = gmt.generate_value(col("some_new_column", nullable=False, default="0"), 1, None, None, self.start, self.end)
        self.assertIsNone(value)

    def test_not_null_unrecognized_column_without_default_warns_and_placeholders(self):
        stderr = StringIO()
        old_stderr, sys.stderr = sys.stderr, stderr
        try:
            value = gmt.generate_value(col("weird_required_field", nullable=False, default=None), 1, None, None, self.start, self.end)
        finally:
            sys.stderr = old_stderr
        self.assertEqual(value, "TODO_FILL_IN")
        self.assertIn("weird_required_field", stderr.getvalue())


if __name__ == "__main__":
    unittest.main()
