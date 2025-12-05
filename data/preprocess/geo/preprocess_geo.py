from __future__ import annotations

from pathlib import Path
from typing import List

import geopandas as gpd
import pandas as pd

from data.preprocess.utils.paths import RAW, CLEAN

RAW_GEO = RAW / "geo"
CLEAN_GEO = CLEAN / "geo"
CLEAN_GEO.mkdir(parents=True, exist_ok=True)


def _with_centroids(gdf: gpd.GeoDataFrame,
                    lon_col: str = "centroid_lon",
                    lat_col: str = "centroid_lat") -> gpd.GeoDataFrame:
    """
    Add centroid lon/lat columns in WGS84.
    """
    if gdf.crs is None:
        gdf = gdf.set_crs(epsg=4326, allow_override=True)
    elif gdf.crs.to_epsg() != 4326:
        gdf = gdf.to_crs(epsg=4326)

    cent = gdf.geometry.centroid
    gdf[lon_col] = cent.x
    gdf[lat_col] = cent.y
    return gdf


# ---------------- States ----------------

def preprocess_us_states() -> gpd.GeoDataFrame:
    """
    Natural Earth admin-1 -> US states subset.

    Input:
        data/raw/geo/us/ne_50m_admin_1_states_provinces.zip
    """
    src = RAW_GEO / "us" / "ne_50m_admin_1_states_provinces.zip"
    g = gpd.read_file(src)

    g = g[g["iso_a2"] == "US"].copy()

    g["state_name"] = g["name"]
    g["state_abbr"] = g["postal"]
    g["state_fips"] = g["fips"].astype(str).str.zfill(2)

    g = _with_centroids(g)

    out_cols = [
        "state_fips",
        "state_abbr",
        "state_name",
        "centroid_lon",
        "centroid_lat",
        "geometry",
    ]
    g = g[out_cols]

    dest = CLEAN_GEO / "us_states.geojson"
    g.to_file(dest, driver="GeoJSON")
    print(f"[GEO] wrote {dest}")
    return g


# --------------- Counties (5 states) ---------------

def _preprocess_ma_counties() -> gpd.GeoDataFrame:
    path = RAW_GEO / "ma" / "COUNTIESSURVEY_POLYM_GENCOAST.shp"
    g = gpd.read_file(path)
    g["state_abbr"] = "MA"
    g["county_name"] = g["COUNTY"].astype(str).str.title()
    g["fips5"] = g["FIPS_STCO"].astype(str).str.zfill(5)
    g = _with_centroids(g)
    return g[["state_abbr", "fips5", "county_name", "centroid_lon", "centroid_lat", "geometry"]]


def _preprocess_ia_counties() -> gpd.GeoDataFrame:
    path = RAW_GEO / "ia" / "Iowa_Counties.geojson"
    g = gpd.read_file(path)
    g["state_abbr"] = "IA"
    g["county_name"] = g["COUNTY"].astype(str).str.title()
    g["fips5"] = g["FIPS"].astype(str).str.zfill(5)
    g = _with_centroids(g)
    return g[["state_abbr", "fips5", "county_name", "centroid_lon", "centroid_lat", "geometry"]]


def _preprocess_nc_counties() -> gpd.GeoDataFrame:
    path = RAW_GEO / "nc" / "North_Carolina_counties.geojson"
    g = gpd.read_file(path)
    g["state_abbr"] = "NC"
    g["county_name"] = g["County"].astype(str).str.title()
    g["fips5"] = g["FIPS"].astype(str).str.zfill(5)
    g = _with_centroids(g)
    return g[["state_abbr", "fips5", "county_name", "centroid_lon", "centroid_lat", "geometry"]]


def _preprocess_il_counties() -> gpd.GeoDataFrame:
    path = RAW_GEO / "il" / "IL_BNDY_County_Py.shp"
    g = gpd.read_file(path)
    g["state_abbr"] = "IL"
    g["county_name"] = g["COUNTY_NAM"].astype(str).str.title()
    g["fips5"] = "17" + g["CO_FIPS"].astype(int).apply(lambda v: f"{v:03d}")
    g = _with_centroids(g)
    return g[["state_abbr", "fips5", "county_name", "centroid_lon", "centroid_lat", "geometry"]]


def _preprocess_wa_counties() -> gpd.GeoDataFrame:
    path = RAW_GEO / "wa" / "WA_County_Boundaries.geojson"
    g = gpd.read_file(path)
    g["state_abbr"] = "WA"
    g["county_name"] = g["JURISDICT_NM"].astype(str)
    g["fips5"] = g["JURISDICT_FIPS_DESG_CD"].astype(int).apply(lambda v: f"{v:05d}")
    g = _with_centroids(g)
    return g[["state_abbr", "fips5", "county_name", "centroid_lon", "centroid_lat", "geometry"]]


def preprocess_counties_selected() -> gpd.GeoDataFrame:
    """
    IA, IL, MA, NC, WA counties combined.
    """
    frames = [
        _preprocess_ma_counties(),
        _preprocess_ia_counties(),
        _preprocess_nc_counties(),
        _preprocess_il_counties(),
        _preprocess_wa_counties(),
    ]
    g = pd.concat(frames, ignore_index=True)
    dest = CLEAN_GEO / "us_counties_selected.geojson"
    g.to_file(dest, driver="GeoJSON")
    print(f"[GEO] wrote {dest}")
    return g


# --------------- WA precincts (2024) ---------------

def preprocess_wa_precincts_2024() -> gpd.GeoDataFrame:
    """
    Washington statewide precincts for 2024 General.

    Input:
        data/raw/geo/wa/precints/Statewide_Precincts_2024General_1.zip
    """
    src = RAW_GEO / "wa" / "precints" / "Statewide_Precincts_2024General_1.zip"
    g = gpd.read_file(src)

    g["state_abbr"] = "WA"
    g["county_fips"] = g["County"].astype(str).str.zfill(5)
    g["county_name"] = g["CountyName"].astype(str)
    g["precinct_number"] = g["PrecinctNu"]
    g["precinct_name"] = g["PrecinctNa"].astype(str)

    g = _with_centroids(g)

    out_cols = [
        "state_abbr",
        "county_fips",
        "county_name",
        "precinct_number",
        "precinct_name",
        "centroid_lon",
        "centroid_lat",
        "geometry",
    ]
    g = g[out_cols]

    dest = CLEAN_GEO / "wa_precincts_2024.geojson"
    g.to_file(dest, driver="GeoJSON")
    print(f"[GEO] wrote {dest}")
    return g


def main():
    preprocess_us_states()
    preprocess_counties_selected()
    preprocess_wa_precincts_2024()


if __name__ == "__main__":
    main()