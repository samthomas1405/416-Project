import React, { useEffect, useMemo, useRef, useState } from "react";
import { MapContainer, TileLayer, Marker, Popup, GeoJSON, useMap } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "leaflet-defaulticon-compatibility/dist/leaflet-defaulticon-compatibility.css";
import "leaflet-defaulticon-compatibility";
import * as d3 from "d3";


function FitToBounds({ bounds }) {
  const map = useMap();
  useEffect(() => {
    if (bounds && map) {
      map.fitBounds(bounds, { padding: [20, 20] });
    }
  }, [map, bounds]);
  return null;
}


//GeoJSON file uses FIPS will keep this incase EAVS uses USPS code
// const FIPS_TO_USPS = {
//   "01": "AL","02": "AK","04": "AZ","05": "AR","06": "CA","08": "CO","09": "CT",
//   "10": "DE","11": "DC","12": "FL","13": "GA","15": "HI","16": "ID","17": "IL",
//   "18": "IN","19": "IA","20": "KS","21": "KY","22": "LA","23": "ME","24": "MD",
//   "25": "MA","26": "MI","27": "MN","28": "MS","29": "MO","30": "MT","31": "NE",
//   "32": "NV","33": "NH","34": "NJ","35": "NM","36": "NY","37": "NC","38": "ND",
//   "39": "OH","40": "OK","41": "OR","42": "PA","44": "RI","45": "SC","46": "SD",
//   "47": "TN","48": "TX","49": "UT","50": "VT","51": "VA","53": "WA","54": "WV",
//   "55": "WI","56": "WY"
// };


// ---- App Shell -------------------------------------------------------------
export default function App() {
  const [route, setRoute] = useState({ view: "us" }); // {view:'us'} | {view:'state', id:'NY'}
  const [activeTab, setActiveTab] = useState("summary"); // 'summary' | 'eavs' | 'registration' | 'equipment'
  const [eavsCategory, setEavsCategory] = useState("Provisional Ballots");

  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100">
      <TopNav onReset={() => { setRoute({ view: "us" }); setActiveTab("summary"); }} />

      {route.view === "us" ? (
        <USLanding onSelectState={(payload)=> {
          //console.log("clicked payload:", payload); 
          const {id, name, bounds} = payload; 
          setRoute({ view: "state", id, name, bounds });
        }} 
        />
      ) : (
        <StateView
          stateId={route.id}
          stateName={route.name}
          initialBounds = {route.bounds}
          activeTab={activeTab}
          onChangeTab={setActiveTab}
          eavsCategory={eavsCategory}
          onChangeEavs={setEAVS}
          onBack={() => setRoute({ view: "us" })}
        />
      )}
    </div>
  );

  function setEAVS(v) { setEavsCategory(v); }
}

// ---- Top Navigation --------------------------------------------------------
function TopNav({ onReset }) {
  return (
    <header className="sticky top-0 z-10 border-b border-neutral-800 bg-neutral-900/70 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-3">
          <div className="h-3 w-3 rounded-full bg-emerald-400" />
          <h1 className="text-lg font-semibold tracking-tight">Elections GUI – Rough Draft</h1>
        </div>
        <button className="rounded-xl border border-neutral-700 px-3 py-1.5 text-sm hover:bg-neutral-800" onClick={onReset}>
          Reset to US
        </button>
      </div>
    </header>
  );
}

// ---- US Landing (Splash) ---------------------------------------------------
function USLanding({ onSelectState }) {
  return (
    <main className="mx-auto grid max-w-7xl grid-cols-1 gap-4 px-4 py-4 md:grid-cols-5">
      <section className="md:col-span-3">
        <Card title="US Map ">
          <div className="h-[460px] w-full overflow-hidden rounded-2xl">
            <LeafletMap center={[39.5, -98.35]} zoom={4}>
              {/* TODO: Replace with US + state GeoJSON layers */}
              {/* <Marker position={[38.9, -77.03]}>
                <Popup>Washington, DC (placeholder)</Popup>
              </Marker> */}
              <USStatesLayer onClickState={(payload)=> onSelectState(payload)} />
            </LeafletMap>
          </div>

        </Card>
      </section>

      <aside className="md:col-span-2 flex flex-col gap-4">
        <Card title="Quick Comparisons (placeholders)">
          <div className="grid grid-cols-2 gap-3 text-sm">
            <Placeholder label="Equipment by State" />
            <Placeholder label="US Equipment Summary" />
            <Placeholder label="Registration Policy Compare" />
            <Placeholder label="Early Voting by Party" />
          </div>
        </Card>
        <Card title="Notes">
          <ul className="list-disc pl-5 text-sm text-neutral-300">
            <li>This is a rough draft; real data and GeoJSON layers will be wired in later.</li>
          </ul>
        </Card>
      </aside>
    </main>
  );
}

// ---- State View ------------------------------------------------------------
function StateView({ stateId, stateName, initialBounds, activeTab, onChangeTab, eavsCategory, onChangeEavs, onBack }) {
  const stateNameComputed = stateName ?? stateId;

  return (
    <main className="mx-auto max-w-7xl px-4 py-4">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <button className="rounded-xl border border-neutral-700 px-3 py-1.5 text-sm hover:bg-neutral-800" onClick={onBack}>
            ← Back
          </button>
          <h2 className="text-base font-semibold tracking-tight">{stateNameComputed}</h2>
          <span className="rounded-md border border-neutral-700 px-2 py-0.5 text-xs text-neutral-300">2024 Regions</span>
        </div>

        <select
          className="rounded-lg border border-neutral-700 bg-neutral-900 px-2 py-1 text-sm"
          value={eavsCategory}
          onChange={(e) => onChangeEavs(e.target.value)}
        >
          {EAVS_CATEGORIES.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-5">
        {/* Map */}
        <section className="md:col-span-3">
          <Card title={`${stateName} Map (Leaflet)`}>
            <div className="h-[420px] w-full overflow-hidden rounded-2xl">
              <LeafletMap center={[39.5, -98.35]} zoom = {6} initialBounds={initialBounds}>
              </LeafletMap>
            </div>
          </Card>
        </section>

        {/* Right panel with tabs */}
        <section className="md:col-span-2 flex flex-col gap-4">
          <Tabs
            value={activeTab}
            onChange={onChangeTab}
            tabs={[
              { id: "summary", label: "State Summary" },
              { id: "eavs", label: "EAVS" },
              { id: "registration", label: "Registration" },
              { id: "equipment", label: "Equipment" },
            ]}
          />

          {activeTab === "summary" && (
            <Card title="At-a-glance (placeholder data)">
              <ul className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm text-neutral-300">
                <li><strong>Population:</strong> 10.1M</li>
                <li><strong>VAP:</strong> 7.8M</li>
                <li><strong>Urban/Suburban/Rural:</strong> 55% / 32% / 13%</li>
                <li><strong>Median Income:</strong> $59k</li>
                <li><strong>Delegation:</strong> 8R / 5D</li>
                <li><strong>Redistricting:</strong> Leg-controlled</li>
              </ul>
            </Card>
          )}

          {activeTab === "eavs" && (
            <Card title={`EAVS: ${eavsCategory}`}>
              <div className="space-y-3">
                <BarChart height={180} data={SAMPLE_BAR_DATA} xKey="label" yKey="value" />
                <Placeholder label="Region table (virtualized)" />
              </div>
            </Card>
          )}

          {activeTab === "registration" && (
            <Card title="Registration">
              <div className="space-y-3">
                <Placeholder label="Choropleth: % Registered" />
                <BubbleChart height={220} data={SAMPLE_BUBBLE_DATA} xKey="x" yKey="y" rKey="r" colorKey="color" />
                <Placeholder label="Roster table (paginated)" />
              </div>
            </Card>
          )}

          {activeTab === "equipment" && (
            <Card title="Equipment (by make/model)">
              <ul className="text-sm text-neutral-300">
                <li>Model A — qty 120 — OS X — VVSG 1.0</li>
                <li>Model B — qty 65 — OS Y — VVSG 2.0</li>
                <li className="text-red-400">Retired: Model C — qty 12</li>
              </ul>
              <div className="mt-3 grid grid-cols-2 gap-3">
                <Placeholder label="History: DRE" />
                <Placeholder label="History: BMD" />
                <Placeholder label="History: Scanner" />
                <Placeholder label="History: DRE+VVPAT" />
              </div>
            </Card>
          )}
        </section>
      </div>
    </main>
  );
}

// ---- Leaflet Map Wrapper ---------------------------------------------------
function LeafletMap({ center, zoom, children, initialBounds }) {
  return (
    <MapContainer center={center} zoom={zoom} className="h-full w-full">
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {initialBounds ? <FitToBounds bounds={initialBounds}/>: null}
      {children}
    </MapContainer>
  );
}

// GeoJSON layer for clickable US states using FIPS -> USPS mapping
function USStatesLayer({ onClickState }) {
  const [geojson, setGeojson] = React.useState(null);
  const map = useMap();

  React.useEffect(() => {
    // Put your file in /public/us-states.json
    fetch("/us-states.json")
      .then((r) => r.json())
      .then(setGeojson)
      .catch((e) => console.error("Failed to load us-states.json", e));
  }, []);

  if (!geojson) return null;

  const baseStyle = {
    weight: 1.2,
    color: "#71717a",
    fillColor: "#18181b",
    fillOpacity: 0.35,
  };

  function onEachFeature(feature, layer) {
    //GeoJSON file format properties.STATE (FIPS as string), properties.NAME (state name)
    const id = feature?.properties?.STATE;         //FIPS number
    const name = feature?.properties?.NAME;  // e.g., "Maine"
    //const usps = FIPS_TO_USPS[fips];                 //Use if the EAVS is using USPS code

    layer.bindTooltip(name, { sticky: true });

    layer.on({
      click: () => {
        if (!id) return;
          const b = layer.getBounds();
          const sw = b.getSouthWest();
          const ne = b.getNorthEast();
          const bounds = [
            [sw.lat, sw.lng],
            [ne.lat, ne.lng],
          ];
          onClickState({id, name, bounds});
      },
      mouseover: (e) => e.target.setStyle({ weight: 2, color: "#e5e7eb", fillOpacity: 0.5 }),
      mouseout:  (e) => e.target.setStyle(baseStyle),
    });
  }

  return (
    <GeoJSON
      data={geojson}
      style={() => baseStyle}
      onEachFeature={onEachFeature}
    />
  );
}




// ---- D3: BarChart ----------------------------------------------------------
function BarChart({ data, xKey, yKey, height = 220, margin = { top: 12, right: 12, bottom: 28, left: 36 } }) {
  const ref = useRef(null);
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const ro = new ResizeObserver((entries) => {
      for (const e of entries) setWidth(e.contentRect.width);
    });
    if (ref.current) ro.observe(ref.current);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    const el = ref.current;
    if (!el || !width) return;

    d3.select(el).selectAll("*").remove();

    const svg = d3.select(el).append("svg").attr("width", width).attr("height", height);
    const innerW = width - margin.left - margin.right;
    const innerH = height - margin.top - margin.bottom;

    const g = svg.append("g").attr("transform", `translate(${margin.left},${margin.top})`);

    const x = d3.scaleBand().domain(data.map((d) => d[xKey])).range([0, innerW]).padding(0.2);
    const y = d3.scaleLinear().domain([0, d3.max(data, (d) => d[yKey]) || 1]).nice().range([innerH, 0]);

    g.append("g").attr("transform", `translate(0,${innerH})`).call(d3.axisBottom(x)).selectAll("text")
      .attr("font-size", 10).attr("transform", "rotate(-20)").style("text-anchor", "end");

    g.append("g").call(d3.axisLeft(y).ticks(4));

    g.selectAll("rect.bar")
      .data(data)
      .join("rect")
      .attr("class", "bar")
      .attr("x", (d) => x(d[xKey]))
      .attr("y", (d) => y(d[yKey]))
      .attr("width", x.bandwidth())
      .attr("height", (d) => innerH - y(d[yKey]))
      .attr("fill", "currentColor")
      .attr("opacity", 0.9);
  }, [data, height, margin, width, xKey, yKey]);

  return <div ref={ref} className="w-full text-indigo-400" />;
}

// ---- D3: BubbleChart -------------------------------------------------------
function BubbleChart({ data, xKey, yKey, rKey, colorKey, height = 240, margin = { top: 12, right: 12, bottom: 32, left: 40 } }) {
  const ref = useRef(null);
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const ro = new ResizeObserver((entries) => {
      for (const e of entries) setWidth(e.contentRect.width);
    });
    if (ref.current) ro.observe(ref.current);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    const el = ref.current;
    if (!el || !width) return;

    d3.select(el).selectAll("*").remove();

    const svg = d3.select(el).append("svg").attr("width", width).attr("height", height);
    const innerW = width - margin.left - margin.right;
    const innerH = height - margin.top - margin.bottom;

    const g = svg.append("g").attr("transform", `translate(${margin.left},${margin.top})`);

    const x = d3.scaleLinear().domain(d3.extent(data, (d) => d[xKey])).nice().range([0, innerW]);
    const y = d3.scaleLinear().domain(d3.extent(data, (d) => d[yKey])).nice().range([innerH, 0]);
    const r = d3.scaleSqrt().domain([0, d3.max(data, (d) => d[rKey]) || 1]).range([2, 16]);
    const color = d3.scaleOrdinal().domain(["R", "D", "U"]).range(["#ef4444", "#3b82f6", "#a3a3a3"]);

    g.append("g").attr("transform", `translate(0,${innerH})`).call(d3.axisBottom(x).ticks(5).tickFormat((d) => `${d}%`));
    g.append("g").call(d3.axisLeft(y).ticks(5).tickFormat((d) => `${d}%`));

    g.selectAll("circle.dot")
      .data(data)
      .join("circle")
      .attr("class", "dot")
      .attr("cx", (d) => x(d[xKey]))
      .attr("cy", (d) => y(d[yKey]))
      .attr("r", (d) => r(d[rKey]))
      .attr("fill", (d) => color(d[colorKey]))
      .attr("fill-opacity", 0.8)
      .attr("stroke", "#111")
      .attr("stroke-width", 0.5)
      .append("title")
      .text((d) => `x:${d[xKey]}%\ny:${d[yKey]}%\nsize:${d[rKey]}`);
  }, [data, height, margin, width, xKey, yKey, rKey, colorKey]);

  return <div ref={ref} className="w-full" />;
}

// ---- UI helpers ------------------------------------------------------------
function Card({ title, children }) {
  return (
    <div className="rounded-2xl border border-neutral-800 bg-neutral-900 p-4 shadow-lg">
      <div className="mb-3 flex items-center justify-between">
        <h3 className="text-sm font-semibold tracking-tight text-neutral-200">{title}</h3>
      </div>
      {children}
    </div>
  );
}

function Tabs({ value, onChange, tabs }) {
  return (
    <div className="rounded-xl border border-neutral-500 bg-neutral-950 p-1">
      <div className="flex flex-wrap gap-1">
        {tabs.map((t) => (
          <button
            key={t.id}
            onClick={() => onChange(t.id)}
            className={
              "rounded-lg px-3 py-1.5 text-sm " +
              (value === t.id ? "bg-neutral-800 text-white" : "text-neutral-300 hover:bg-neutral-900")
            }
          >
            {t.label}
          </button>
        ))}
      </div>
    </div>
  );
}

function Placeholder({ label }) {
  return (
    <div className="flex h-24 items-center justify-center rounded-xl border border-dashed border-neutral-700 text-xs text-neutral-400">
      {label}
    </div>
  );
}

// ---- Sample (temporary) data ----------------------------------------------
const SAMPLE_STATES = [
  { id: "NY", name: "New York" },
  { id: "GA", name: "Georgia" },
  { id: "MI", name: "Michigan" },
  { id: "WI", name: "Wisconsin" },
  { id: "AZ", name: "Arizona" },
  { id: "PA", name: "Pennsylvania" },
];

const EAVS_CATEGORIES = [
  "Provisional Ballots",
  "Active vs Inactive Voters",
  "Pollbook Deletions",
  "Mail Ballot Rejections",
];

const SAMPLE_BAR_DATA = [
  { label: "E2a", value: 12 },
  { label: "E2b", value: 9 },
  { label: "E2c", value: 15 },
  { label: "E2d", value: 7 },
  { label: "E2e", value: 11 },
  { label: "E2f", value: 5 },
];

const SAMPLE_BUBBLE_DATA = [
  { x: 35, y: 6, r: 10, color: "R" },
  { x: 52, y: 14, r: 18, color: "D" },
  { x: 47, y: 9, r: 12, color: "U" },
  { x: 60, y: 20, r: 22, color: "R" },
  { x: 28, y: 4, r: 8, color: "D" },
];
