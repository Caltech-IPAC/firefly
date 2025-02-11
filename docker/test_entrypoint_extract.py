"""Test the extract entrypoint function."""

import unittest
from pathlib import Path
import shutil
import tempfile
from entrypoint import extract


class TestExtractFunction(unittest.TestCase):
    """Test the extract entrypoint function

    Args:
        unittest (object): TestCase class for unit testing
    """

    def setUp(self):
        """Set up the test environment"""
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.source = Path(self.temp_dir.name) / "source"
        self.destination = Path(self.temp_dir.name) / "destination"
        self.source.mkdir()
        (self.source / "sample.txt").write_text("dummy content")
        shutil.make_archive(str(self.source / "sample"), "zip", self.source)

    def test_extract_success(self):
        """Test the extract function with a successful extraction"""
        # Call the function
        extract(self.source, self.destination, "/mock/prefix", "zip")

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
            extract(self.source, self.destination, "/mock/prefix", "zip")

    def test_extract_copy_secondary_files(self):
        """Test the extract function with additional files in the source directory"""
        additional = self.source / "sample"
        additional.mkdir()
        (additional / "sample2.txt").write_text("dummy content")

        # Call the function
        extract(self.source, self.destination, "/", "zip")

        # Assertions
        output = self.destination / "sample"
        self.assertTrue(output.exists())
        self.assertTrue(output.is_dir())
        self.assertTrue((output / "sample2.txt").exists())


if __name__ == "__main__":
    unittest.main()
