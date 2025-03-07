import duckdb 
import healpy as hp 
import numpy as np 
import time 
import argparse
 
# Add parameters
parser = argparse.ArgumentParser(description="test healpy.")
parser.add_argument("infile", type=str, help="Input file")  # required positional argument
args = parser.parse_args()

 
def ang2pix(nside: int, theta: float, phi: float) -> int: 
    return int(hp.ang2pix(nside, theta, phi, True)) 
 
def deg2pix(order, ra, dec) -> int: 
    # Convert the order to nside (order is log2(nside)) 
    nside = 2 ** order 
 
    # Convert RA, Dec (in degrees) to theta, phi in radians 
    theta = np.radians(90 - dec)  # Dec to theta 
    phi = np.radians(ra)          # RA to phi 
 
    # Calculate the pixel index 
    pixel_index = hp.ang2pix(nside, theta, phi, nest=True) 
    return int(pixel_index) 
 
conn = duckdb.connect() 
duckdb.create_function("deg2pix", deg2pix) 
 
start_time = time.time()
 
sql = f"select pixel, count() from (select deg2pix(12,ra,dec) as pixel from '{args.infile}') group by pixel order by pixel"
copySql = f"COPY ({sql}) TO 'healpy.csv' WITH (FORMAT CSV, HEADER)"
 
# Verify the UDF by calling it 
print( duckdb.sql(copySql) ) 
 
end_time = time.time()
 
# Calculate the elapsed time 
elapsed_time = end_time - start_time 
print(f"Elapsed time: {elapsed_time}") 
print(f"SQL executed: {copySql}") 
 
# Close the connection 
conn.close() 
                                                                                                            
