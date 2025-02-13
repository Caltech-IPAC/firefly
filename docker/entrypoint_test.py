"""This module contains the unit tests for the entrypoint script."""

import unittest
from unittest.mock import patch
import os
import shutil
from pathlib import Path
import tempfile
import entrypoint  # Import your script


class TestEntrypoint(unittest.TestCase):
    """Entrypoint Test Class.

    Args:
        unittest (obj): The unittest class.
    """

    @patch.dict(
        os.environ,
        {
            "PROPS": (
                "logging.level=DEBUG;"
                "app.mode=production;"
                "key=value;;with semicolon;"
                'key2=value with "quotes";'
                "key3=value with 'single-quotes'"
            )
        },
    )
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
            """-Dkey3='value with '"'"'single-quotes'"'"''""",
        ]

        for option in expected_options:
            self.assertIn(option, result)

    @patch.dict(
        os.environ,
        {
            "PROPS_logging__level": "DEBUG",
            "PROPS_app__mode": "production",
            "PROPS_special__key": "value with spaces",
            "PROPS_with__semicolon": "value; with semicolon",
            "PROPS_json": '{"abc": 123, "def": "value"}',
        },
    )
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
            '-Djson=\'{"abc": 123, "def": "value"}\'',
        ]

        for option in expected_options:
            self.assertIn(option, result)

    def setUp(self):
        """Set up the test environment"""
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.source = Path(self.temp_dir.name) / "source"
        self.destination = Path(self.temp_dir.name) / "destination"
        self.source.mkdir()
        (self.source / "sample.txt").write_text("content")
        shutil.make_archive(str(self.source / "sample"), "zip", self.source)
        # Rename the zip file to war
        os.rename(self.source / "sample.zip", self.source / "sample.war")

    def test_extract_success(self):
        """Test the extract function with a successful extraction"""
        # Call the function
        entrypoint.extract(self.source, self.destination, "/mock/prefix")

        # Assertions
        output = self.destination / "mock#prefix#sample"
        self.assertTrue(output.exists())
        self.assertTrue(output.is_dir())
        self.assertTrue((output / "sample.txt").exists())

        # Cleanup output directory
        shutil.rmtree(output)

    def test_extract_source_not_exist(self):
        """Test the extract function when the source does not exist"""
        # Remove source directory to simulate non-existence
        shutil.rmtree(self.source)
        # Call the function
        with self.assertRaises(FileNotFoundError):
            entrypoint.extract(self.source, self.destination, "/mock/prefix")

    def test_extract_copy_secondary_files(self):
        """Test the extract function with additional files in the source directory"""
        additional = self.source / "sample"
        additional.mkdir()
        (additional / "sampler.txt").write_text("contents")

        # Call the function
        entrypoint.extract(self.source, self.destination, "/")

        # Assertions
        output = self.destination / "sample"
        self.assertTrue(output.exists())
        self.assertTrue(output.is_dir())
        self.assertTrue((output / "sampler.txt").exists())


if __name__ == "__main__":
    unittest.main()
