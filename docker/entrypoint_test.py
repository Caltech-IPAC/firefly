import unittest
from unittest.mock import patch
import os
import entrypoint  # Import your script

class TestEntrypoint(unittest.TestCase):


    @patch.dict(os.environ, {
        "PROPS": (
            "logging.level=DEBUG;"
            "app.mode=production;"
            "key=value;;with semicolon;"
            "key2=value with \"quotes\";"
            "key3=value with 'single-quotes'"
        )
    })
    def test_addMultiPropsEnvVar(self):
        """Test addMultiPropsEnvVar() processes PROPS correctly."""
        result = entrypoint.add_multi_props_env_var()
        print("Test: addMultiPropsEnvVar()")
        print("Actual Output  :", result)

        # Expected formatted JVM options
        expected_options = [
            "-Dlogging.level=DEBUG",
            "-Dapp.mode=production",
            "-Dkey='value;with semicolon'",
            "-Dkey2='value with \"quotes\"'",
            """-Dkey3='value with '"'"'single-quotes'"'"''"""
        ]

        for option in expected_options:
            self.assertIn(option, result)

    @patch.dict(os.environ, {
        "PROPS_logging__level": "DEBUG",
        "PROPS_app__mode": "production",
        "PROPS_special__key": "value with spaces",
        "PROPS_with__semicolon": "value; with semicolon",
        "PROPS_json": '{"abc": 123, "def": "value"}'
    })
    def test_addSinglePropEnvVars(self):
        """Test addSinglePropEnvVars() processes PROPS_* environment variables correctly."""
        result = entrypoint.add_single_prop_env_vars()
        print("Test: test_addSinglePropEnvVars()")
        print("Actual Output  :", result)

        # Expected formatted JVM options
        expected_options = [
            "-Dlogging.level=DEBUG",
            "-Dapp.mode=production",
            "-Dspecial.key='value with spaces'",
            "-Dwith.semicolon='value; with semicolon'",
            "-Djson='{\"abc\": 123, \"def\": \"value\"}'"
        ]

        for option in expected_options:
            self.assertIn(option, result)

if __name__ == "__main__":
    unittest.main()
