import React, {useEffect, useMemo, useRef, useState,} from "react"
import { MapContainer, GeoJSON, useMap, TileLayer} from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import * as d3 from "d3";

//temporary since we do not have dummy data for county boundaries. 
// later on when we develop DB maybe we get rid of this
let CACHE_US_COUNTIES = null;
const DETAILED_STATES = new Set(["17","25","37","19","53"]);

function isDetailed(fips) {
  return DETAILED_STATES.has(String(fips));
}



function FitToBounds({ bounds }) {
  const map = useMap();
  useEffect(() => {
    if (bounds && map) {
      map.fitBounds(bounds, { padding: [20, 20] });
    }
  }, [map, bounds]);
  return null;
}

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
      <div className="mx-auto flex max-w-screen-2xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-3">
          <img src = "https://cdn.nba.com/logos/nba/1610612740/primary/L/logo.svg" alt = "Pelicans logo" className = "h-12 w-12"/>
          <h1 className="text-lg font-semibold tracking-tight">Pelicans</h1>
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
    <main className="mx-auto grid max-w-screen-2xl grid-cols-1 gap-4 px-4 py-4 md:grid-cols-5">
      <section className="md:col-span-3">
        <Card title="US Map ">
          <div className="h-[460px] w-full overflow-hidden rounded-2xl">
            <LeafletMap center={[39.5, -96.35]} zoom={4}>
              <USStatesLayer onClickState={(payload)=> onSelectState(payload)} />
            </LeafletMap>
          </div>

        </Card>
      </section>

      <aside className="md:col-span-2 flex flex-col gap-4">
        {/*maybe change to a tab on the left */}
        <Card title="Splash buttons (placeholders)"> 
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

function SelectedStateLayer({ stateId, style }) {
  const [gj, setGj] = React.useState(null);
  const map = useMap();

  React.useEffect(() => {
    fetch("/us-states.json")
      .then(r => r.json())
      .then(setGj)
      .catch(e => console.error("load us-states.json failed", e));
  }, []);

  React.useEffect(() => {
    try {
      const tmp = L.geoJSON(data)
      const b = tmp.getBounds()
      map.fitBounds(b, {padding: [2,2], animate:false, maxZoom:10 })
      map.setMaxBounds(b.pad(0.02))
    } catch {}
  }, [stateId,map]);

  if (!gj) return null;

  const feature = gj.features?.find(f => f?.properties?.STATEFP === stateId);
  if (!feature) return null;

  const data = { type: "FeatureCollection", features: [feature] };

  const baseStyle = {
    weight: 1,
    color: "#000000ff",
    fillColor: "#959eb3ff",
    fillOpacity: 0.1,
    ...style,
  };

  return <GeoJSON data={data} style={() => baseStyle} />;
}

// ---- State View ------------------------------------------------------------
function StateView({ stateId, stateName, initialBounds, activeTab, onChangeTab, eavsCategory, onChangeEavs, onBack }) {
  const stateNameComputed = stateName ?? stateId;

  return (
    <main className="mx-auto max-w-screen-2xl px-4 py-4" style={{height: "calc(100vh - 64px)"}}>
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
                {isDetailed(stateId) ? <DetailedCountyLayer stateFips={stateId} source="/us-counties.json"/>:
                <SelectedStateLayer stateId={stateId}/>
                }
                
              </LeafletMap>
            </div>
          </Card>
        </section>

        {/*might need waiting on piazza respnse*/}
        {/* <section className="md:col-span-2 flex flex-col gap-4">
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
        </section> */}
        <section className="md:col-span-2">
          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            {/* SUMMARY */}
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

            {/* EAVS */}
            <Card title={`EAVS: ${eavsCategory}`}>
              <div className="space-y-3">
                <BarChart height={150} data={SAMPLE_BAR_DATA} xKey="label" yKey="value" />
              </div>
            </Card>

            {/* REGISTRATION */}
            <Card title="Registration">
              <div className="space-y-3">
                <BubbleChart height={170} data={SAMPLE_BUBBLE_DATA} xKey="x" yKey="y" rKey="r" colorKey="color" />
              </div>
            </Card>

            {/* EQUIPMENT */}
            <Card title="Equipment (by make/model)">
              <EquipmentTable rows={SAMPLE_EQUIPMENT} pageSize={3} />
            </Card>
          </div>
        </section>

      </div>
    </main>
  );
}

//----- CountyView ------------------------------------------------------------
function DetailedCountyLayer({
  stateFips,                  // e.g., "17" for Illinois
  source = "/us-counties.json",
  dataMap,                    // Map<GEOID, number> (needed; for choropleth) but need data
  bins = 5,
  color = d3.interpolateBlues,
  baseStyle = {
    weight: 0.8,
    color: "#94A3B8",
    fillColor: "#022983ff",
    fillOpacity: 0.95,
  },
  onFeatureClick,             // implement tmr
}) {
  const [gj, setGj] = React.useState(CACHE_US_COUNTIES);
  const map = useMap();

  // load (and cache) the national file
  React.useEffect(() => {
    if (CACHE_US_COUNTIES) return;
    fetch(source)
      .then(r => r.json())
      .then(json => { CACHE_US_COUNTIES = json; setGj(json); })
      .catch(e => console.error("Failed to load counties:", e));
  }, [source]);

  const feats = React.useMemo(() => {
    const features = gj?.features || [];
    return features.filter(f => String(f?.properties?.STATEFP) === String(stateFips));
  }, [gj, stateFips]);

  // fit to county bounds on load
  React.useEffect(() => {
    if (!feats?.length) return;
    const tmp = L.geoJSON({ type: "FeatureCollection", features: feats });
    try { map.fitBounds(tmp.getBounds(), { padding: [20, 20] }); } catch {}
  }, [feats, map]);

  if (!feats?.length) return null;

  let styleFn = () => baseStyle;
  if (dataMap instanceof Map) {
    const vals = feats.map(f => dataMap.get(String(f.properties?.GEOID))).filter(v => v != null);
    const domain = (vals.length ? d3.extent(vals) : [0, 1]);
    const palette = d3.range(bins).map(i => color((i + 1) / bins));
    const scale = d3.scaleQuantize().domain(domain).range(palette);

    styleFn = (feature) => {
      const geoid = String(feature.properties?.GEOID);
      const v = dataMap.get(geoid);
      return { ...baseStyle, fillColor: v == null ? "#E5E7EB" : scale(v) };
    };
  }

  function onEachFeature(feature, layer) {
    const name = feature.properties?.NAME ?? feature.properties?.GEOID;
    const geoid = String(feature.properties?.GEOID);
    const v = dataMap?.get(geoid);
    const txt = v == null ? "No data" : (typeof v === "number" ? d3.format(".1%")(v) : String(v));

    layer.bindTooltip(`${name}\n${txt}`, { sticky: true });
    layer.on({
      click: () => onFeatureClick && onFeatureClick(feature),
      mouseover: (e) => e.target.setStyle({ ...styleFn(feature), weight: 1.2, color: "#E2E8F0" }),
      mouseout:  (e) => e.target.setStyle(styleFn(feature)),
    });
  }

  return (
    <GeoJSON
      data={{ type: "FeatureCollection", features: feats }}
      style={styleFn}
      onEachFeature={onEachFeature}
    />
  );
}

// ---- Leaflet Map Wrapper ---------------------------------------------------
function LeafletMap({ center, zoom, children, initialBounds }) {
  return (
    <MapContainer center={center} zoom={zoom} className="h-full w-full bg-[#F4F3EE]" attributionControl ={false} preferCanvas = {true} zoomControl={true} scrollWheelZoom = {true} dragging = {true}>
      <TileLayer
      url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      />

      {initialBounds ? <FitToBounds bounds={initialBounds}/>: null}
      {children}
    </MapContainer>
  );
}

function USStatesLayer({ onClickState }) {
  const [geojson, setGeojson] = React.useState(null);
  const map = useMap();

  React.useEffect(() => {
    fetch("/us-states.json")
      .then((r) => r.json())
      .then(setGeojson)
      .catch((e) => console.error("Failed to load us-states.json", e));
  }, []);

  if (!geojson) return null;

  function style(feature){
    const fips = String(feature?.properties?.STATEFP);
    const isDetailed = DETAILED_STATES.has(fips);
    return{
      weight: isDetailed? 2: 0.5,
      color:"#000000ff",
      fillcolor: "#959eb3ff",
      fillOpacity:0.1
    }
  }

  function onEachFeature(feature, layer) {
    layer.setStyle(style(feature));
    const id = feature?.properties?.STATEFP;//FIPS number
    const name = feature?.properties?.NAME; // e.g., "Maine"

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
      mouseover: (e) => {
        e.target.setStyle({...style(feature),  weight: style(feature).weight + 0.8, color: "#0b2880ff", fillOpacity: 0.1 });
        e.target.bringToFront();
      },
      mouseout:  (e) => e.target.setStyle(style(feature)),
    });
  }

  return (
    <GeoJSON
      data={geojson}
      style={() => style}
      onEachFeature={onEachFeature}
    />
  );
}




// --------BarChart ----------------------------------------------------------
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

// --------BubbleChart -------------------------------------------------------
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

function EquipmentTable({ rows, pageSize = 6 }) {
  const [page, setPage] = React.useState(0);
  const [sortBy, setSortBy] = React.useState({ key: "model", dir: "asc" });

  const sorted = React.useMemo(() => {
    const data = rows.slice();
    const { key, dir } = sortBy;
    data.sort((a, b) => {
      const va = a[key], vb = b[key];
      if (typeof va === "number" && typeof vb === "number") {
        return dir === "asc" ? va - vb : vb - va;
      }
      return dir === "asc"
        ? String(va).localeCompare(String(vb))
        : String(vb).localeCompare(String(va));
    });
    return data;
  }, [rows, sortBy]);

  const pages = Math.max(1, Math.ceil(sorted.length / pageSize));
  const start = page * pageSize;
  const current = sorted.slice(start, start + pageSize);

  function toggleSort(key) {
    setPage(0);
    setSortBy(s => (s.key === key ? { key, dir: s.dir === "asc" ? "desc" : "asc" } : { key, dir: "asc" }));
  }

  const Th = ({ colKey, children, align = "left", width }) => (
    <th
      onClick={() => toggleSort(colKey)}
      className={`p-2 text-${align} cursor-pointer select-none`}
      style={width ? { width } : undefined}
      title="Click to sort"
    >
      <span className="inline-flex items-center gap-1">
        {children}
        {sortBy.key === colKey ? (sortBy.dir === "asc" ? "▲" : "▼") : ""}
      </span>
    </th>
  );

  return (
    <div className="rounded-xl border border-neutral-800">
      <table className="w-full text-sm">
        <thead className="bg-neutral-900">
          <tr>
            <Th colKey="model" width="34%">Model</Th>
            <Th colKey="qty" align="right" width="14%">Qty</Th>
            <Th colKey="os" width="20%">OS</Th>
            <Th colKey="vvsg" width="16%">VVSG</Th>
            <Th colKey="status" width="16%">Status</Th>
          </tr>
        </thead>
        <tbody>
          {current.map((r, i) => (
            <tr key={r.id ?? `${r.model}-${i}`} className="border-t border-neutral-800">
              <td className="p-2">{r.model}</td>
              <td className="p-2 text-right">{r.qty}</td>
              <td className="p-2">{r.os}</td>
              <td className="p-2">{r.vvsg}</td>
              <td className={`p-2 ${r.status === "Retired" ? "text-red-400" : "text-neutral-300"}`}>
                {r.status}
              </td>
            </tr>
          ))}
          {current.length === 0 && (
            <tr><td className="p-3 text-neutral-400" colSpan={5}>No equipment.</td></tr>
          )}
        </tbody>
      </table>

      <div className="flex items-center justify-between p-2 text-xs bg-neutral-900">
        <span>
          Page {page + 1} / {pages} · {sorted.length} item{sorted.length === 1 ? "" : "s"}
        </span>
        <div className="flex gap-1">
          <button
            className="rounded border border-neutral-700 px-2 py-1 disabled:opacity-50"
            disabled={page === 0}
            onClick={() => setPage(p => Math.max(0, p - 1))}
          >
            Prev
          </button>
          <button
            className="rounded border border-neutral-700 px-2 py-1 disabled:opacity-50"
            disabled={page + 1 >= pages}
            onClick={() => setPage(p => Math.min(pages - 1, p + 1))}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}


// ---- Sample (temporary) data ----------------------------------------------

const SAMPLE_EQUIPMENT = [
  { id: "A-1", model: "Model A", qty: 120, os: "OS X", vvsg: "1.0", status: "Active" },
  { id: "B-1", model: "Model B", qty: 65,  os: "OS Y", vvsg: "2.0", status: "Active" },
  { id: "C-1", model: "Model C", qty: 12,  os: "OS W", vvsg: "1.0", status: "Retired" },
  { id: "D-1", model: "Model D", qty: 40,  os: "OS Z", vvsg: "2.0", status: "Active" },
  { id: "E-1", model: "Model E", qty: 22,  os: "OS X", vvsg: "2.0", status: "Active" },
  { id: "F-1", model: "Model F", qty: 9,   os: "OS Y", vvsg: "1.0", status: "Retired" },
  { id: "G-1", model: "Model G", qty: 71,  os: "OS Z", vvsg: "2.0", status: "Active" },
];


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
