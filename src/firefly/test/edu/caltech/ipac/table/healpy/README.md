# HEALPix Indexing in DuckDB vs. Healpy Library

This tool provides a test framework for comparing HEALPix indexing implemented in **DuckDB** using a User Defined Function (UDF) versus the **Healpy** library in Python.

## How to Run the Test

### 1. Prepare Input Data
- Copy a **CSV** or **Parquet** file containing `ra` (Right Ascension) and `dec` (Declination) columns into this directory.

### 2. Run Healpy in Docker
Execute the following command:
```sh
docker compose run healpy
```
This will start a **bash prompt** inside the container.

### 3. Run Healpy Conversion
Inside the **Docker container**, run:
```sh
python3 run_healpy.py {file_name}
```
Replace `{file_name}` with your actual file name (CSV or Parquet).

**Output:**  
This command will generate a `healpy.csv` file containing HEALPix indices computed using Healpy.

### 4. Run DuckDB Implementation
Exit the Docker container and return to your **host terminal**.

Start a **DuckDB session** by running:
```sh
duckdb
```

Inside DuckDB session:
1. **Load the DuckDB HEALPix UDF**  
   Copy and paste the SQL script from:
   ```
   src/firefly/java/edu/caltech/ipac/firefly/resources/healpix-java.sql
   ```
   into the DuckDB session and execute it.

2. **Run the Same Query as Healpy**
    - Look for the `SQL executed:` output from `run_healpy.py`
    - Copy & paste the SQL into DuckDB
    - Replace **`healpy.csv`** with your desired output file name.

### 5. Compare Results
Once both methods generate HEALPix indices, compare the outputs:
```sh
diff healpy.csv {your_duckdb_output}.csv
```
If the files match, it confirms that both implementations produce identical results.


