import React, { useEffect, useRef, useState } from "react";
import { MapContainer, GeoJSON, useMap, TileLayer } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import * as d3 from "d3";
import axios from 'axios';

// --- MUI core ---
import {
  ThemeProvider,
  createTheme,
  CssBaseline,
  AppBar,
  Toolbar,
  IconButton,
  Button,
  Typography,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Divider,
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Stack,
  Grid,
  MenuItem,
  Select,
  InputLabel,
  FormControl,
} from "@mui/material";
import{DataGrid} from "@mui/x-data-grid"

// --- MUI icons ---
import MenuIcon from "@mui/icons-material/Menu";

import RestartAltIcon from "@mui/icons-material/RestartAlt"

import CloseIcon from "@mui/icons-material/Close"




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

function useE2LegendMap(data, stateId) {
  // Prefer the state's labels; if missing, fall back to any detailed state present
  const firstAvailable =
    data?.GUI03_provisional_bar?.[stateId] ??
    data?.GUI03_provisional_bar?.["25"] ??
    data?.GUI03_provisional_bar?.["19"] ??
    data?.GUI03_provisional_bar?.["53"] ??
    data?.GUI03_provisional_bar?.["37"] ??
    data?.GUI03_provisional_bar?.["17"] ??
    [];
  const map = new Map();
  for (const d of firstAvailable) map.set(d.key, d.label);
  return map;
}

function useE1aChoropleth(data, stateId, binsFallback = 7) {
  const s = data?.GUI05_provisional_choropleth_e1a;
  const entry = s?.[stateId];
  const raw = entry?.values || {};
  const bins = s?.bins ?? binsFallback;

  // convert to Map<GEOID, number>
  const m = React.useMemo(() => {
    const mm = new Map();
    for (const k in raw) mm.set(String(k), Number(raw[k]));
    return mm;
  }, [raw]);

  // compute domain & colors (monochrome)
  const vals = Array.from(m.values());
  const domain = vals.length ? d3.extent(vals) : [0, 1];
  const colors = d3.range(bins).map(i => d3.interpolateBlues((i + 1) / bins));
  const scale = d3.scaleQuantize().domain(domain).range(colors);

  return { dataMap: m, bins, scale, colors, domain };
}

function ChoroplethLegend({ domain, colors, format = d3.format(",") }) {
  if (!colors?.length) return null;
  const [min, max] = domain || [0, 1];
  return (
    <Box sx={{
      position: "absolute", right: 12, bottom: 12,
      px: 1.25, py: 1, borderRadius: 1.5, bgcolor: "rgba(17,19,24,0.8)",
      border: "1px solid #2b3240", fontSize: 12
    }}>
      <Box sx={{ mb: .5, color: "neutral.300" }}>Total Provisional (E1a)</Box>
      <Stack direction="row" spacing={0.5} alignItems="center">
        {colors.map((c, i) => (
          <Box key={i} sx={{ width: 16, height: 10, bgcolor: c, borderRadius: .5 }} />
        ))}
      </Stack>
      <Box sx={{ display: "flex", justifyContent: "space-between", color: "neutral.400", mt: .5 }}>
        <span>{format(min)}</span><span>{format(max)}</span>
      </Box>
    </Box>
  );
}


const theme = createTheme({
  palette:{
    mode: "dark",
    background: { default: "#0a0a0a", paper: "#111318" },
  },
  shape: {borderRadius:14},
  components: {
    MuiCard:{styleOverrides:{root: {border:"1px solid #262b36"}}},
    MuiDrawer: { styleOverrides: { paper: { backgroundColor: "#101217" } } },
    MuiAppBar: { styleOverrides: { root: { background: "rgba(12,14,18,.8)", backdropFilter: "blur(6px)" } } },

  },

});

export default function AppShell(){
  return(
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  );
}


// ---- App Shell -------------------------------------------------------------
function App() {
  const [route, setRoute] = useState({ view: "us" }); // {view:'us'} | {view:'state', id:'NY'}
  const [activeTab, setActiveTab] = useState("summary"); // 'summary' | 'eavs' | 'registration' | 'equipment'
  const [eavsCategory, setEavsCategory] = useState("Provisional Ballots"); 
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [panelOpen, setPanelOpen] = useState(false);
  const [panelView, setPanelView] = useState(null);

  return (
    <Box sx={{ height: "100vh", bgcolor: "background.default", color: "text.primary" }}>
      <TopNav onReset={() => { setRoute({ view: "us" }); setActiveTab("summary"); setDrawerOpen(false); setPanelOpen(false)}}
        showMenu = {route.view == "us"} 
        onOpenMenu={()=> setDrawerOpen(true)}
      />

      <LeftDrawer 
        open={drawerOpen} 
        onClose= {() => setDrawerOpen(false)} 
        onPick= {(id) => {setDrawerOpen(false); setPanelView(id); setPanelOpen(true)}}
      />
      {route.view === "us" ? (
        <USLanding 
          drawerOpen= {drawerOpen} 
          panelOpen={panelOpen} 
          panelView={panelView} 
          onClosePanel = {()=>setPanelOpen(false)} 
          onSelectState={(payload)=> {
            //console.log("clicked payload:", payload); 
            const {id, name, bounds} = payload; 
            setRoute({ view: "state", id, name, bounds });
            setPanelOpen(false);
            } 
          } 
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
          onBack={() => {setRoute({ view: "us" }); setPanelOpen(false);}}
        />
      )}
    </Box>
    
  );

  function setEAVS(v) { setEavsCategory(v); }
}

// ---- Top Navigation --------------------------------------------------------
function TopNav({ onReset, showMenu =false, onOpenMenu}) {
  return (
    <AppBar position="sticky" elevation={0} sx={{ borderBottom: "1px solid #1f2430" }}>
      <Toolbar variant="dense" sx={{ minHeight: 48 }}>
        <Stack direction="row" alignItems="center" spacing={1}>
          <Box
            component="img"
            src="https://cdn.nba.com/logos/nba/1610612740/primary/L/logo.svg"
            alt="Pelicans logo"
            sx={{ height: 28, width: 28 }}
          />
          <Typography variant="subtitle1" fontWeight={600}>Pelicans</Typography>
        </Stack>
        <Box sx={{ flex: 1 }} />
          {showMenu && (
            <IconButton color="inherit" size="small" onClick={onOpenMenu}>
              <MenuIcon />
            </IconButton>
        )}
        <Button
          size="small"
          variant="outlined"
          startIcon={<RestartAltIcon />}
          onClick={onReset}
          sx={{ textTransform: "none", ml: 1, borderColor: "#2b3240" }}
        >
          Reset to US
        </Button>
      </Toolbar>
    </AppBar>
  );
}

function LeftDrawer({ open, onClose, onPick }) {

  return (

    <Drawer
      open={open}
      onClose={onClose}
      anchor="left"
      PaperProps={{ sx: { width: 300, borderRight: "1px solid #1f2430", mt: "48px" } }}
    >
      <Stack

        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{ px: 2, py: 1.5 }}
      >
        <Typography variant="subtitle2" sx={{ color: "neutral.300" }}>
          Splash Page Options
        </Typography>
        <IconButton size="small" onClick={onClose}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </Stack>
      <Divider />
      <List dense disablePadding>
        <ListItemButton onClick={() => onPick("equipmentByState")}>
          <ListItemText primary="State Voting Equipment" />
        </ListItemButton>
        <ListItemButton onClick={() => onPick("equipSummary")}>
          <ListItemText primary="Voting Equipment Summary" />
        </ListItemButton>
        <ListItemButton onClick={() => onPick("compareRD")}>
          <ListItemText primary="(D)Massachusetts vs (R)Iowa" />
        </ListItemButton>
        <ListItemButton onClick={() => onPick("regOptInOut")}>
          <ListItemText
            primary={
              <Stack spacing={0.5}>
                <div>(opt-in)North Carolina vs</div>
                <div>(opt-out SD registration)Illinois vs</div>
                <div>(opt-out no SD registration) Massachusetts</div>
              </Stack>
            }
          />
        </ListItemButton>
      </List>
    </Drawer>
  );
}

function useDummyData() {
  const [data, setData] = React.useState(null);
  useEffect(() => {
    fetch("/dummy-data.json").then(r => r.json()).then(setData).catch(console.error);
  }, []);
  return data;
}

function PanelContent({ view }) {
  const data = useDummyData();
  if (!view) {
    return <Typography variant="body2" color="text.secondary">Select an item from the menu.</Typography>;
  }
  if (!data) {
    return <Typography variant="body2" color="text.secondary">Loading…</Typography>;
  }
  switch (view) {
    case "equipmentByState":
      return <EquipByStateTable rows={data.GUI12_equipment_by_state_2024 || []} />;
    case "equipSummary":
      return <EquipSummaryTable rows={data.GUI13_equipment_summary_2024 || []} />;
    case "compareRD":
      return(
        <>
          <CompareRDBlock
      data15={data.GUI15_compare_republican_democratic}
      data22={data.GUI22_registration_republican_democratic}
      data23={data.GUI23_early_voting_party_domination}
    />
        </>
      ) 
    case "regOptInOut":
      return <RegOptInOutTable obj={data.GUI21_registration_optin_optout || []} />;
    default:
      return <Typography variant="body2" color="text.secondary">Not implemented.</Typography>;
  }
}

function RegRDTable({ obj, tableProps = {} }) {
  if (!obj) return <Typography variant="body2" color="text.secondary">No data available.</Typography>;
  const { dense } = tableProps;

  const rep = obj.republican ?? {};
  const dem = obj.democratic ?? {};
  const fNum = d3.format(",");
  const fPct1 = d3.format(".1%");

  function calc(state) {
    const vap = Number(state.total_vap ?? 0);
    const reg = Number(state.registered ?? 0);
    const to  = Number(state.turnout_count ?? 0);
    return {
      vap, reg, to,
      regRate: vap ? reg / vap : 0,
      toVAP: vap ? to / vap : 0,
      toReg: reg ? to / reg : 0,
    };
  }

  const repStats = calc(rep);
  const demStats = calc(dem);

  const rows = [
    { metric: "State", rep: rep.state, dem: dem.state },
    { metric: "Voting Age Population (VAP)", rep: fNum(repStats.vap), dem: fNum(demStats.vap) },
    { metric: "Registered", rep: fNum(repStats.reg), dem: fNum(demStats.reg) },
    { metric: "Registration Rate", rep: fPct1(repStats.regRate), dem: fPct1(demStats.regRate) },
    { metric: "Turnout", rep: fNum(repStats.to), dem: fNum(demStats.to) },
    { metric: "Turnout / VAP", rep: fPct1(repStats.toVAP), dem: fPct1(demStats.toVAP) },
    { metric: "Turnout / Registered", rep: fPct1(repStats.toReg), dem: fPct1(demStats.toReg) },
  ];

  return (
    <TableContainer component={Paper} variant="outlined" className="miniTable">
      <Table size={dense ? "small" : "medium"}>
        <TableHead>
          <TableRow>
            <TableCell>Metric</TableCell>
            <TableCell align="center">Republican</TableCell>
            <TableCell align="center">Democratic</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r, i) => (
            <TableRow key={i}>
              <TableCell sx={{ fontWeight: 600 }}>{r.metric}</TableCell>
              <TableCell align="center">{r.rep}</TableCell>
              <TableCell align="center">{r.dem}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function EarlyVotingTable({ obj, tableProps = {}}) {
  if (!obj) return <Typography variant="body2" color="text.secondary">No data available.</Typography>;
  const { dense } = tableProps;

  const dem = obj.dem_dominated ?? {};
  const rep = obj.rep_dominated ?? {};
  const fNum = d3.format(",");
  const fPct1 = d3.format(".1%");

  function calc(state) {
    const tot = Number(state.total_votes ?? 0);
    const eip = Number(state.early_in_person ?? 0);
    const mail = Number(state.early_by_mail ?? 0);
    const box = Number(state.drop_box_returns ?? 0);
    return {
      tot, eip, mail, box,
      pctEIP: tot ? eip / tot : 0,
      pctMail: tot ? mail / tot : 0,
      pctBox: tot ? box / tot : 0,
      pctTotal: tot ? (eip + mail + box) / tot : 0,
    };
  }

  const repStats = calc(rep);
  const demStats = calc(dem);

  const rows = [
    { metric: "State", rep: rep.state, dem: dem.state },
    { metric: "Total Votes", rep: fNum(repStats.tot), dem: fNum(demStats.tot) },
    { metric: "Early In-Person", rep: fNum(repStats.eip), dem: fNum(demStats.eip) },
    { metric: "% Early In-Person", rep: fPct1(repStats.pctEIP), dem: fPct1(demStats.pctEIP) },
    { metric: "Early by Mail", rep: fNum(repStats.mail), dem: fNum(demStats.mail) },
    { metric: "% Early by Mail", rep: fPct1(repStats.pctMail), dem: fPct1(demStats.pctMail) },
    { metric: "Drop Box Returns", rep: fNum(repStats.box), dem: fNum(demStats.box) },
    { metric: "% Drop Box Returns", rep: fPct1(repStats.pctBox), dem: fPct1(demStats.pctBox) },
    { metric: "Total Early Voting %", rep: fPct1(repStats.pctTotal), dem: fPct1(demStats.pctTotal) },
  ];

  return (
    <TableContainer component={Paper} variant="outlined" className="miniTable">
      <Table size={dense ? "small" : "medium"}>
        <TableHead>
          <TableRow>
            <TableCell>Metric</TableCell>
            <TableCell align="center">Republican-Dominated</TableCell>
            <TableCell align="center">Democratic-Dominated</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r, i) => (
            <TableRow key={i}>
              <TableCell sx={{ fontWeight: 600 }}>{r.metric}</TableCell>
              <TableCell align="center">{r.rep}</TableCell>
              <TableCell align="center">{r.dem}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}


function EquipSummaryTable({ rows }) {
  // Normalize snake_case → camelCase and make a stable id
  const dgRows = (rows || []).map((r, i) => ({
    id: `${r.provider}-${r.model}-${i}`,
    provider: r.provider,
    model: r.model,
    qty: r.qty,
    age: r.age,
    os: r.os,
    certification: r.certification,
    scanRate: r.scan_rate,          // e.g. 80  → "80/min"
    // If your JSON's error_rate is 0.5 meaning 50%, leave as-is.
    // If it means 0.5%, set errorRate: r.error_rate / 100 instead.
    errorRate: r.error_rate,
    reliability: r.reliability,     // e.g. 0.97 → "97%"
    quality: r.quality,             // letter grade
  }));

  const columns = [
    { field: "provider", headerName: "Provider", flex: 1.1, minWidth: 140 },
    { field: "model", headerName: "Model", flex: 1.2, minWidth: 180 },
    { field: "qty", headerName: "Qty", type: "number", flex: 0.5, minWidth: 90 },
    { field: "age", headerName: "Age (yrs)", type: "number", flex: 0.5, minWidth: 110 },
    { field: "os", headerName: "OS", flex: 0.8, minWidth: 120 },
    { field: "certification", headerName: "Certification", flex: 1, minWidth: 150 },
    {
      field: "scanRate",
      headerName: "Scan Rate",
      flex: 0.6,
      minWidth: 110,
      valueFormatter: ({ value } = {}) => (value != null ? `${value}/min` : ""),
    },
    {
      field: "errorRate",
      headerName: "Error Rate",
      type: "number",
      flex: 0.6,
      minWidth: 110,
      // Auto-handle either 0–1 or 0–100 style values
      valueFormatter: ({ value } = {}) => {
        if (value == null) return "";
        const v = value > 1 ? value / 100 : value; // 50 → 0.5
        return d3.format(".2%")(v);
      },
    },
    {
      field: "reliability",
      headerName: "Reliability",
      type: "number",
      flex: 0.6,
      minWidth: 110,
      valueFormatter: ({ value } = {}) => (value != null ? d3.format(".0%")(value) : ""),
    },
    {
      field: "quality",
      headerName: "Quality",
      flex: 0.5,
      minWidth: 100,
      renderCell: (p) => (
        <span
          style={{
            padding: "2px 8px",
            borderRadius: 8,
            border: "1px solid #2b3240",
            background: "#14161c",
            fontSize: 12,
          }}
        >
          {p.value ?? ""}
        </span>
      ),
    },
  ];

  return (
    <Card title="US Voting Equipment Summary (2024)">
      <Paper variant="outlined" sx={{ height: 520, overflow: "hidden" }}>
        <DataGrid
          rows={dgRows}
          columns={columns}
          density="compact"
          disableRowSelectionOnClick
          pageSizeOptions={[10, 25, 50, 100]}
          initialState={{
            pagination: { paginationModel: { pageSize: 25 } },
            sorting: {
              sortModel: [
                { field: "provider", sort: "asc" },
                { field: "model", sort: "asc" },
              ],
            },
          }}
          sx={{
            border: 0,
            "& .MuiDataGrid-columnHeaders": { borderBottom: "1px solid #2b3240" },
            "& .MuiDataGrid-columnHeaderTitle": {
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            },
          }}
        />
      </Paper>
    </Card>
  );
}

function DropBoxBubbleChart({ data, stateId, height = 220 }) {
  // Your JSON: stateId -> [{ region, pct_rep (0-1), pct_dropbox (0-1), dropbox_votes }, ...]
  const rows = data?.GUI24_dropbox_bubbles?.[stateId] ?? [];

  const chart = rows.map(p => ({
    // convert 0–1 to 0–100, with one decimal precision
    x: Math.round(((p.pct_rep ?? 0) * 100) * 10) / 10,
    y: Math.round(((p.pct_dropbox ?? 0) * 100) * 10) / 10,
    r: Math.max(1, p.dropbox_votes ?? 1),
    color: (p.pct_rep ?? 0) >= 0.5 ? "R" : "D",
    label: p.region ?? ""
  }));

  // y padding: if values are small (e.g., 3–8%), show a 0–10% band
  const yVals = chart.map(d => d.y);
  const yMax = Math.max(10, Math.ceil((d3.max(yVals) || 0) * 1.25));
  const yDom = [0, Math.min(100, yMax)];

  return (
    <Card title="Drop-box Voting vs % Republican (2024)">
      <BubbleChart
        data={chart}
        xKey="x"
        yKey="y"
        rKey="r"
        colorKey="color"
        height={height}
        xDomain={[0, 100]}
        yDomain={yDom}
        xTickFormat={(d) => `${d}%`}
        yTickFormat={(d) => `${d}%`}
      />
      <div className="mt-1 text-[11px] leading-4 text-neutral-400">
        x: % Republican vote • y: % votes returned via drop boxes (C3a) • size: drop-box votes.
      </div>
    </Card>
  );
}




function CompareRDBlock({ data15, data22, data23 }) {
  return (
    <Box sx={{ px: 0.5 }}>
      <Grid
        container
        spacing={3}
        sx={{
          // card look
          "& .miniCard": {
            p: 1.25,
            borderRadius: 2,
            border: "1px solid #2b3240",
            backgroundColor: "rgba(17,19,24,1)",
            boxShadow: "0 0 0 1px rgba(0,0,0,0.2) inset",
          },
          "& .miniTitle": {
            fontSize: 12,
            fontWeight: 700,
            letterSpacing: 0.25,
            color: "neutral.200",
            mb: 0.75,
          },

          // compact table text + padding
          "& .miniTable .MuiTableCell-root": {
            py: 0.5,
            px: 1,
            fontSize: 12,
            lineHeight: 1.25,
          },
          "& .miniTable thead .MuiTableCell-root": {
            fontWeight: 700,
            color: "neutral.200",
          },

          // sticky header for scrollable tables
          "& .miniTable thead .MuiTableCell-root": {
            position: "sticky",
            top: 0,
            zIndex: 1,
            backgroundColor: "#111318",
            borderBottom: "1px solid #2b3240",
          },
        }}
      >
        {/* Row 1: two cards */}
        <Grid item xs={12} md={6}>
          <Box className="miniCard">
            <div className="miniTitle">Compare (R) vs (D) — 2024</div>
            <Box sx={{ minHeight: "190px", overflow: "auto" }}>
              <CompareRDTable obj={data15} tableProps={{ dense: true }} />
            </Box>
          </Box>
        </Grid>

        <Grid item xs={12} md={6}>
          <Box className="miniCard">
            <div className="miniTitle">Registration &amp; Turnout — 2024</div>
            <Box sx={{ maxHeight: { xs: 260, md: 230 }, overflow: "auto" }}>
              <RegRDTable obj={data22} tableProps={{ dense: true }} />
            </Box>
          </Box>
        </Grid>

        {/* Row 2: full width */}
        <Grid item xs={12}>
          <Grid container justifyContent="center">
            <Grid item xs={12} md={8}>
              <Box className="miniCard">
                <div className="miniTitle">Early Voting — 2024</div>
                <Box sx={{ maxHeight: { xs: 320, md: 260 }, overflow: "auto", right:"200px"}}>
                  <EarlyVotingTable obj={data23} tableProps={{ dense: true }} />
                </Box>
              </Box>
            </Grid>
          </Grid>
        </Grid>
      </Grid>
    </Box>
  );
}



function CompareRDTable({ obj, tableProps = {} }) {
  if (!obj) return <Typography variant="body2" color="text.secondary">No data available.</Typography>;

  const {dense} = tableProps;

  const rep = obj.republican ?? {};
  const dem = obj.democratic ?? {};

  // Each row = metric name + rep value + dem value
  const rows = [
    { metric: "State", rep: rep.state, dem: dem.state },
    { metric: "Felony Voting Rights", rep: rep.felony_rights, dem: dem.felony_rights },
    { metric: "% Mail Ballots", rep: d3.format(".0%")(rep.pct_mail ?? 0), dem: d3.format(".0%")(dem.pct_mail ?? 0) },
    { metric: "% Drop Box Ballots", rep: d3.format(".0%")(rep.pct_dropbox ?? 0), dem: d3.format(".0%")(dem.pct_dropbox ?? 0) },
    { metric: "Turnout %", rep: d3.format(".0%")(rep.turnout_pct ?? 0), dem: d3.format(".0%")(dem.turnout_pct ?? 0) },
  ];

  return (
    <TableContainer component={Paper} variant="outlined" className="miniTable">
      <Table size={dense ? "small" : "medium"}>
        <TableHead>
          <TableRow>
            <TableCell>Metric</TableCell>
            <TableCell align="center">Republican</TableCell>
            <TableCell align="center">Democratic</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((r, i) => (
            <TableRow key={i}>
              <TableCell sx={{ fontWeight: 600 }}>{r.metric}</TableCell>
              <TableCell align="center">{r.rep}</TableCell>
              <TableCell align="center">{r.dem}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function RegOptInOutTable({ obj }) {
  if (!obj) return <Typography variant="body2" color="text.secondary">No data available.</Typography>;

  const fNum = d3.format(",");
  const fPct1 = d3.format(".1%");
  const fPct0 = d3.format(".0%");

  const items = [
    { key: "opt_in", label: "Opt-in (SDR)", ...obj.opt_in },
    { key: "opt_out_same_day", label: "Opt-out (Same-day)", ...obj.opt_out_same_day },
    { key: "opt_out_no_sdr", label: "Opt-out (No SDR)", ...obj.opt_out_no_sdr },
  ];

  const rows = items.map((s) => {
    const vap = Number(s.total_vap ?? 0);
    const reg = Number(s.registered ?? 0);
    const to  = Number(s.turnout_count ?? 0);
    const regRate = vap ? reg / vap : 0;
    const toVAP   = vap ? to / vap  : 0;
    const toReg   = reg ? to / reg  : 0;
    return {
      id: s.key,
      category: s.label,
      state: s.state,
      vap, reg, to, regRate, toVAP, toReg,
    };
  });

  return (
    <Card title="Voter Registration: Opt-in vs Opt-out (2024)">
      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ width: 160 }}>Category</TableCell>
              <TableCell>State</TableCell>
              <TableCell align="right">VAP</TableCell>
              <TableCell align="right">Registered</TableCell>
              <TableCell align="right">Reg. Rate</TableCell>
              <TableCell align="right">Turnout</TableCell>
              <TableCell align="right">Turnout / VAP</TableCell>
              <TableCell align="right">Turnout / Registered</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((r) => (
              <TableRow key={r.id}>
                <TableCell sx={{ fontWeight: 600 }}>{r.category}</TableCell>
                <TableCell>{r.state}</TableCell>
                <TableCell align="right">{fNum(r.vap)}</TableCell>
                <TableCell align="right">{fNum(r.reg)}</TableCell>
                <TableCell align="right">{fPct1(r.regRate)}</TableCell>
                <TableCell align="right">{fNum(r.to)}</TableCell>
                <TableCell align="right">{fPct1(r.toVAP)}</TableCell>
                <TableCell align="right">{fPct1(r.toReg)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <div className="mt-2 text-[11px] leading-4 text-neutral-400">
        Rates shown as proportions: Registration = Registered ÷ VAP; Turnout/VAP = Turnout ÷ VAP; Turnout/Registered = Turnout ÷ Registered.
      </div>
    </Card>
  );
}


function EquipByStateTable({ rows }) {
  const columns = [
    { field: "state", headerName: "State", flex: 1.1, minWidth: 140 },
    { field: "dre_no_vvpat", headerName: "DRE (no VVPAT)", type: "number", flex:0.9, width: 130 },
    { field: "dre_vvpat", headerName: "DRE (VVPAT)", type: "number", flex:0.9, width: 120 },
    { field: "bmd", headerName: "BMD", type: "number", flex:0.9, width: 60 },
    { field: "scanner", headerName: "Scanner", type: "number", flex:0.9, width: 100 },
  ];

  // DataGrid needs an id; use your stateFips
  const dgRows = (rows || []).map(r => ({ id: r.stateFips, ...r }));

  return (
    <Paper variant="outlined" sx={{ height: 420, overflow: "hidden" }}>
      <DataGrid
        rows={dgRows}
        columns={columns}
        disableRowSelectionOnClick
        pageSizeOptions={[10, 25, 50]}
        initialState={{
          pagination: { paginationModel: { pageSize: 10 } },
          sorting: { sortModel: [{ field: "scanner", sort: "desc" }] }
        }}
        sx={{
          border: 0,
          "& .MuiDataGrid-columnHeaders": { borderBottom: "1px solid #2b3240" },
        }}
      />
    </Paper>
  );
}

function EquipmentSummaryTable({ data, stateId }) {
  const rows = data?.GUI06_state_equipment_summary?.[stateId] ?? [];
  const dgRows = rows.map((r, i) => ({ id: r.id ?? i, ...r }));

  const columns = [
    { field: "makeModel", headerName: "Make & Model", flex: 1.4, minWidth: 220 },
    { field: "type", headerName: "Type", flex: 0.8, minWidth: 120 },
    { field: "os", headerName: "OS", flex: 0.6, minWidth: 90 },
    { field: "certification", headerName: "Certification", flex: 1, minWidth: 140 },
    { field: "qty", headerName: "Qty", type: "number", flex: 0.4, width: 70 },
    { field: "scanRate", headerName: "Scan Rate",  flex: 0.5, width: 90,valueFormatter: ({ value } = {}) => (value != null ? `${value}/min` : " ") },
    { field: "errorRate", headerName: "Error Rate", flex: 0.5, width: 90,valueFormatter: ({ value } = {}) => (value != null ? d3.format(".2%")(value) : " ") },
    { field: "reliability", headerName: "Reliability", flex: 0.5, width: 90, valueFormatter: ({ value } = {}) => (value != null ? d3.format(".0%")(value) : " ")},
    { field: "age", headerName: "Age (yrs)", type: "number", flex: 0.4, width: 70 },
  ];

  return (
    <Card title="State Voting Equipment Summary">
      <Paper variant="outlined" sx={{ height: 200, overflow: "hidden" }}>
        <DataGrid
          rows={dgRows}
          columns={columns}
          disableRowSelectionOnClick
          density="compact"
          pageSizeOptions={[5, 10, 25]}
          initialState={{
            pagination: { paginationModel: { pageSize: 5 } },
            sorting: { sortModel: [{ field: "type", sort: "asc" }] }
          }}
          getRowClassName={(p) => (p?.row?.retired ? "row-retired" : "")}
          sx={{
            border: 0,
            "& .MuiDataGrid-columnHeaders": { borderBottom: "1px solid #2b3240" },
            "& .MuiDataGrid-columnHeaderTitle": { overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" },
            "& .MuiDataGrid-virtualScroller": { overflowX: "hidden !important" },
            "& .row-retired .MuiDataGrid-cell": { color: "#f87171" } // red for retired
          }}
        />
      </Paper>
    </Card>
  );
}

// ---- US Landing (Splash) ---------------------------------------------------
function USLanding({ onSelectState, drawerOpen, panelOpen, panelView, onClosePanel}) {
  return (
    <Box sx={{position: "relative", height: "calc(100vh - 48px)"}}>
      <Box sx={{position: "absolute", inset: 0}}>
        <LeafletMap center={[37.75, -96.5]} zoom = {4.6} resizeSignal={panelOpen}>
          <USStatesLayer onClickState={(payload) => onSelectState(payload)}/>
        </LeafletMap>
      </Box>
      <RightSlidePanel open={panelOpen} onClose={onClosePanel} view = {panelView}/>
    </Box>
  );
}

function RightSlidePanel({ open, onClose, view }) {
  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{ sx: { width: {xs:'100%', sm:560, md:720,lg:920}, maxWidth:'92vw', borderLeft: "1px solid #1f2430", mt: "48px" } }}
    >
      <Stack sx={{ height: "100%" }}>
        <Stack
          direction="row"
          alignItems="center"
          justifyContent="space-between"
          sx={{ px: 2, py: 1.5, borderBottom: "1px solid #1f2430" }}
        >
          <Typography variant="subtitle2">{titleForView(view)}</Typography>
          <IconButton size="small" onClick={onClose}>
            <CloseIcon fontSize="small" />
          </IconButton>
        </Stack>
        <Box sx={{ p: 2, overflow: "auto" }}>
          <PanelContent view={view} />
        </Box>
      </Stack>
    </Drawer>
  );
}

function titleForView(view) {
  switch (view) {
    case "equipmentByState": return "US Equipment by State (2024)";
    case "equipSummary": return "US Equipment Summary (2024)";
    case "compareRD":    return "Compare R vs D States";
    case "regOptInOut":  return "Registration: Opt-in vs Opt-out";
    case "regRD":        return "Registration: R vs D";
    case "earlyVoting":  return "Early Voting: Party Domination";
    default: return "Details";
  }
}


function SelectedStateLayer({ stateId, style }) {
  const [gj, setGj] = React.useState(null);
  const map = useMap();

  React.useEffect(() => {
    // fetch("/us-states.json")
    axios.get("http://localhost:8080/api/json/us-states")
      .then(res => setGj(res.data))
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

function EavsPicker({ value, onChange, stateId }) {
  const BASE = [
    "Provisional Ballots",
    "Active vs Inactive Voters",
  ];
  const CATS = React.useMemo(
    () => (String(stateId) === "53" ? [...BASE, "Registered Voters"] : BASE),
    [stateId]
  );

  const [equipment, setEquipment] = React.useState([]);

  React.useEffect(() => {
    // fetch("/us-states.json")
    axios.get(`http://localhost:8080/api/equipment/${stateId}`)
      .then(res => setEquipment(res.data))
      .catch(e => console.error("load equipment", e));
  }, []);

  return (
    <FormControl size="small" sx={{ minWidth: 240 }}>
      <InputLabel id="eavs-cat">EAVS Category</InputLabel>
      <Select
        labelId="eavs-cat"
        label="EAVS Category"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        {CATS.map((c) => (
          <MenuItem key={c} value={c}>{c}</MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}


function useActiveChoropleth(data, stateId, binsFallback = 7) {
  const s = data?.GUI07_active_voters_map;
  const entry = s?.[stateId];
  const raw = entry?.values || {};
  const bins = s?.bins ?? binsFallback;

  const m = React.useMemo(() => {
    const mm = new Map();
    for (const k in raw) mm.set(String(k), Number(raw[k])); // 0–1 fraction of active
    return mm;
  }, [raw]);

  const vals = Array.from(m.values());
  const domain = vals.length ? d3.extent(vals) : [0, 1];
  const colors = d3.range(bins).map(i => d3.interpolateBlues((i + 1) / bins));

  return { dataMap: m, bins, colors, domain };
}

function ActiveVotersBar({ data, stateId, height }) {
  const src = data?.GUI07_active_voters?.[stateId]
           ?? data?.GUI07_active_voters?.["25"] ?? null;

  const t = src?.totals ?? { active: 0, inactive: 0, total: 0 };
  const chart = [
    { label: "Active",   value: t.active },
    { label: "Inactive", value: t.inactive },
    { label: "Total",    value: t.total },
  ];

  return (
    <Card title="Active vs Inactive Voters (2024)">
      <BarChart height={height} data={chart} xKey="label" yKey="value" />
    </Card>
  );
}

function ActiveVotersTable({ data, stateId }) {
  const src = data?.GUI07_active_voters?.[stateId]
           ?? data?.GUI07_active_voters?.["25"] ?? null;

  const rows = (src?.regions ?? []).map((r, i) => ({
    id: r.id ?? i, ...r,
    pctActive: r.total ? r.active / r.total : 0
  }));

  const columns = [
    { field: "region", headerName: "Region", flex: 1.1, minWidth: 110 },
    { field: "active", headerName: "Active", type: "number", flex: 0.8, minWidth: 90 },
    { field: "inactive", headerName: "Inactive", type: "number", flex: 0.8, minWidth: 90 },
    { field: "total", headerName: "Total", type: "number", flex: 0.8, minWidth: 90 },
    {
      field: "pctActive",
      headerName: "% Active",
      type: "number",
      flex: 0.7,
      minWidth: 90,
      valueFormatter: (p) => p?.value != null ? d3.format(".1%")(p.value) : ""
    },
  ];

  // compact height + no footer so it fits above the equipment table
  return (
    <Card title="Active/Inactive by EAVS region">
      <Paper variant="outlined" sx={{ height: 240, overflow: "hidden" }}>
        <DataGrid
          rows={rows}
          columns={columns}
          density="compact"
          disableRowSelectionOnClick
          hideFooter
          sx={{
            border: 0,
            "& .MuiDataGrid-columnHeaders": { borderBottom: "1px solid #2b3240" },
            "& .MuiDataGrid-columnHeaderTitle": {
              overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap"
            }
          }}
        />
      </Paper>
    </Card>
  );
}



function ProvisionalBar({ data, stateId }) {
  const rows = data?.GUI03_provisional_bar?.[stateId] ?? [];
  const legend = useE2LegendMap(data, stateId);

  // Your BarChart uses {label, value}; keep axis short (E2a..other),
  // but add <title> for tooltips with long labels.
  const chartData = rows.map(d => ({ label: d.key, value: d.value, _title: legend.get(d.key) || d.label }));

  return (
    <Card title="Provisional ballots by category (E2a–E2i + Other)">
      <BarChart
        height={150}
        margin={{ top: 6, right: 6, bottom: 18, left: 28 }}
        data={chartData}
        xKey="label"
        yKey="value"
      />
      {/* simple legend under the chart */}
      <div className="mt-1 flex flex-wrap items-center gap-1.5">
       {rows.map(r => (
         <span
           key={r.key}
           title={`${legend.get(r.key) || r.label} — ${r.value}`}
           className="inline-flex items-center rounded-md border border-neutral-700 bg-neutral-800/60 px-1.5 py-0.5
                      text-[10px] leading-4 text-neutral-300"
         >
           <span className="font-mono mr-1">{r.key}</span>
           <span className="opacity-80 tabular-nums">{r.value}</span>
         </span>
       ))}
     </div>
    </Card>
  );
}

function ProvisionalTable({ data, stateId }) {
  const legend = useE2LegendMap(data, stateId);

  // Pull rows (fallback to MA so you always see something)
  const raw = data?.GUI04_provisional_table?.[stateId]
    ?? data?.GUI04_provisional_table?.["25"] ?? [];

  // Column order from schema (fallback to full default)
  const schemaCols = data?.GUI04_provisional_table?.schema?.cols ??
    ["region","E2a","E2b","E2c","E2d","E2e","E2f","E2g","E2h","E2i","other","total"];

  // The E2/other fields we sum
  const CATEGORY_FIELDS = schemaCols.filter((c) => /^E2[a-i]$|^other$/.test(c));

  // 1) Precompute total for every data row (except marker TOTAL row)
  const bodyRows = raw.filter(r => r?.region && r.region !== "TOTAL").map((r, idx) => {
    const total = CATEGORY_FIELDS.reduce((s, k) => s + (parseFloat(r?.[k]) || 0), 0);
    return { id: r.id ?? r.region ?? idx, ...r, total };
  });

  // 2) Compute or replace the "TOTAL" row
  const totalsByField = Object.fromEntries(
    CATEGORY_FIELDS.map((k) => [k, bodyRows.reduce((s, r) => s + (parseFloat(r?.[k]) || 0), 0)])
  );
  const grandTotal = CATEGORY_FIELDS.reduce((s, k) => s + (totalsByField[k] || 0), 0);
  const totalRow = { id: "TOTAL", region: "TOTAL", ...totalsByField, total: grandTotal };

  const dgRows = [...bodyRows, totalRow];

  // 3) Build columns (short headers; long tooltip in description)
  const columns = schemaCols.map((col) => {
    if (col === "region") {
      return { field: "region", headerName: "Region", flex: 1.1, minWidth: 110 };
    }
    if (col === "total") {
      return { field: "total", headerName: "Total", type: "number", flex: 0.8, minWidth: 100 };
    }
    // E2a..E2i + other
    return {
      field: col,
      headerName: col.toUpperCase(),  // compact header
      description: legend.get(col) || (col === "other" ? "Other Reasons" : col), // full tooltip
      type: "number",
      flex: 0.75,
      minWidth: 90,
    };
  });

  return (
    <Card title="Provisional ballot details by EAVS region">
      <Paper variant="outlined" sx={{ height: 203, overflow: "hidden" }}>
        <DataGrid
          rows={dgRows}
          columns={columns}
          disableRowSelectionOnClick
          pageSizeOptions={[5, 10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 5 } } }}
          density="compact"
          sx={{
            border: 0,
            "& .MuiDataGrid-columnHeaders": { borderBottom: "1px solid #2b3240" },
            "& .MuiDataGrid-columnHeaderTitle": { overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" },
            "& .MuiDataGrid-virtualScroller": { overflowX: "hidden !important" },
            // bold TOTAL row
            "& .MuiDataGrid-row:last-of-type .MuiDataGrid-cell": { fontWeight: 600 },
          }}
        />
      </Paper>
    </Card>
  );
}

/** ---------- GUI-17: choropleth hook for % registered ---------- */
function useRegisteredChoropleth(data, stateId, binsFallback = 7) {
  // Expect: data.GUI17_registration_map = { "53": { values: { GEOID: 0.82, ... }, bins: 7 } }
  const s = data?.GUI17_registration_map;
  const entry = s?.[stateId];
  const raw = entry?.values || {};
  const bins = s?.bins ?? binsFallback;

  const m = React.useMemo(() => {
    const mm = new Map();
    for (const k in raw) mm.set(String(k), Number(raw[k])); // 0–1 fraction registered
    return mm;
  }, [raw]);

  const vals = Array.from(m.values());
  const domain = vals.length ? d3.extent(vals) : [0, 1];
  const colors = d3.range(bins).map(i => d3.interpolateBlues((i + 1) / bins));

  return { dataMap: m, bins, colors, domain };
}

/** ---------- GUI-17: registration table ---------- */
function RegistrationTable({ data, stateId }) {
  // Expect: data.GUI17_registration_table["53"] = { regions: [{ id, region, registered, dem, rep, unaff, other, total }...] }
  const src = data?.GUI17_registration_table?.[stateId] ?? { regions: [] };
  const rows = (src.regions || []).map((r, i) => ({
    id: r.id ?? i,
    ...r,
    regRate: r.total ? r.registered / r.total : 0
  }));

  const fNum = d3.format(",");
  const fPct1 = d3.format(".1%");

  const columns = [
    { field: "region", headerName: "Region", flex: 1.1, minWidth: 120 },
    { field: "registered", headerName: "Registered", type: "number", flex: 0.9, minWidth: 120, valueFormatter: p => fNum(p.value ?? 0) },
    { field: "dem", headerName: "Dem", type: "number", flex: 0.7, minWidth: 90, valueFormatter: p => fNum(p.value ?? 0) },
    { field: "rep", headerName: "Rep", type: "number", flex: 0.7, minWidth: 90, valueFormatter: p => fNum(p.value ?? 0) },
    { field: "unaff", headerName: "Unaff.", type: "number", flex: 0.7, minWidth: 100, valueFormatter: p => fNum(p.value ?? 0) },
    { field: "other", headerName: "Other", type: "number", flex: 0.7, minWidth: 90, valueFormatter: p => fNum(p.value ?? 0) },
    { field: "total", headerName: "Total", type: "number", flex: 0.9, minWidth: 110, valueFormatter: p => fNum(p.value ?? 0) },
    { field: "regRate", headerName: "Reg. %", type: "number", flex: 0.7, minWidth: 90, valueFormatter: p => fPct1(p.value ?? 0) },
  ];

  return (
    <Card title="Registered Voters by EAVS Region (2024)">
      <Paper variant="outlined" sx={{ height: 300, overflow: "hidden" }}>
        <DataGrid
          rows={rows}
          columns={columns}
          density="compact"
          pageSizeOptions={[5, 10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 5 } } }}
          disableRowSelectionOnClick
          sx={{
            border: 0,
            "& .MuiDataGrid-columnHeaders": { borderBottom: "1px solid #2b3240" },
            "& .MuiDataGrid-columnHeaderTitle": { overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }
          }}
        />
      </Paper>
    </Card>
  );
}

/** ---------- GUI-19: voter list drawer (Washington only) ---------- */
function RegisteredVotersTable({ data, stateId, countyGEOID }) {
  // Hooks MUST be top-level (no conditional/early returns before them)
  const [party, setParty] = React.useState("ALL"); // ALL | R | D

  // Data plumbing
  const counties = data?.GUI19_registered_voters?.["53"]?.counties ?? {};
  const key = String(countyGEOID ?? "").padStart(5, "0");
  const votersAll = counties[key]?.voters ?? [];

  const voters = React.useMemo(
    () => votersAll.filter(v => (party === "ALL" ? true : v?.party === party)),
    [votersAll, party]
  );

  const rows = voters.map((v, i) => ({
    id: i,
    name: `${v.first_name ?? ""} ${v.last_name ?? ""}`.trim(),
    party: v.party ?? "",
    address: [v.address_1, v.city, v.state, v.zip].filter(Boolean).join(", "),
    dob: v.dob ?? "",
  }));

  const columns = [
    { field: "name", headerName: "Name", flex: 1.4, minWidth: 180 },
    { field: "party", headerName: "Party", flex: 0.5, minWidth: 80 },
    { field: "address", headerName: "Address", flex: 1.6, minWidth: 220 },
    { field: "dob", headerName: "DOB", flex: 0.6, minWidth: 110 },
  ];

  return (
    <Card title={`Registered Voters${countyGEOID ? ` (County ${key})` : ""}`}>
      {/* If not WA (53), show an info message but keep the render path identical */}
      {stateId !== "53" ? (
        <div className="text-xs text-neutral-400 px-1 py-2">
          Registered voter list is only available for Washington in this demo.
        </div>
      ) : (
        <>
          <div className="mb-2 flex items-center gap-8">
            <div className="text-xs text-neutral-400">
              {countyGEOID
                ? `Showing ${rows.length.toLocaleString()} voter${rows.length === 1 ? "" : "s"}`
                : "Click a county to load its voter list"}
            </div>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel id="party-filter">Party Filter</InputLabel>
              <Select
                labelId="party-filter"
                label="Party Filter"
                value={party}
                onChange={(e) => setParty(e.target.value)}
              >
                <MenuItem value="ALL">All</MenuItem>
                <MenuItem value="R">Republican</MenuItem>
                <MenuItem value="D">Democratic</MenuItem>
              </Select>
            </FormControl>
          </div>

          <Paper variant="outlined" sx={{ height: 320, overflow: "hidden" }}>
            <DataGrid
              rows={rows}
              columns={columns}
              density="compact"
              disableRowSelectionOnClick
              pageSizeOptions={[10, 25, 50]}
              initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
              sx={{
                border: 0,
                "& .MuiDataGrid-columnHeaders": { borderBottom: "1px solid #2b3240" },
                "& .MuiDataGrid-columnHeaderTitle": {
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                },
              }}
            />
          </Paper>
        </>
      )}
    </Card>
  );
}




// ---- State View ------------------------------------------------------------
// ---- State View ------------------------------------------------------------
function StateView({ stateId, stateName, initialBounds, eavsCategory, onChangeEavs, onBack }) {
  const data = useDummyData();
  const stateNameComputed = stateName ?? stateId;
  const isWA = stateId === "53";

  const [selectedCounty, setSelectedCounty] = React.useState(null);

  const activeMode = eavsCategory === "Active vs Inactive Voters";
  const registeredMode = eavsCategory === "Registered Voters"; // WA-only option

  // If user leaves WA while "Registered Voters" is selected, reset to Provisional
  React.useEffect(() => {
    if (String(stateId) !== "53" && registeredMode) {
      onChangeEavs("Provisional Ballots");
    }
  }, [stateId, registeredMode, onChangeEavs]);

  // sizes you can tweak
  const LAYOUT = {
    MAP_W: { md: 260, lg: 300 },
    MAP_H: { xs: 260, md: 520, lg: 560 },
    BAR_H: { xs: 120, md: 130, lg: 140 },
  };

  // Maps/Legends (Provisional E1a + Active % + optional Registered %)
  const { dataMap: e1Map, colors: e1Colors, domain: e1Domain } = useE1aChoropleth(data, stateId);
  const { dataMap: actMap, colors: actColors, domain: actDomain } = useActiveChoropleth(data, stateId);

  // Registered choropleth (GUI-17) — safe alias
  const useRegChoropleth =
    typeof useRegisteredChoropleth === "function"
      ? useRegisteredChoropleth
      : function fallbackUseRegisteredChoropleth() {
          return { dataMap: new Map(), colors: [], domain: [0, 1] };
        };

  const { dataMap: regMap, colors: regColors, domain: regDomain } =
    useRegChoropleth(data, stateId);

  const showChoropleth = isDetailed(stateId) && (
    (registeredMode && regMap.size > 0) ||
    (activeMode     && actMap.size > 0) ||
    (!registeredMode && !activeMode && e1Map.size > 0)
  );

  // GUI-24: show Drop Box bubble only for IA (19) or MA (25)
  const showDropBox = ["19", "25"].includes(String(stateId));

  // Grid template: on md+ put bubble (col 2) next to equipment (col 3)
  const areasXS = `"map" "bar" "table" ${showDropBox ? `"bubble"` : ""} "equip"`.trim();
  const areasMD = showDropBox
    ? `"map bar table"
       "map bubble equip"`
    : `"map bar table"
       "map equip equip"`;

  return (
    <Box sx={{ px: 1, py: 1, maxWidth: 1600, mx: "auto" }}>
      {/* header */}
      <Box sx={{ display: "grid", gridTemplateColumns: "1fr auto", alignItems: "center", mb: 0.5, gap: 2 }}>
        <Stack direction="row" spacing={1} alignItems="center">
          <Button variant="outlined" size="small" onClick={onBack}>← Back</Button>
          <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 600 }}>{stateNameComputed}</Typography>
          <Box sx={{ px: 1, py: 0.25, border: "1px solid #2b3240", borderRadius: 1, fontSize: 12 }}>
            2024 Regions
          </Box>
        </Stack>
        <EavsPicker value={eavsCategory} onChange={onChangeEavs} stateId={stateId} />
      </Box>

      {/* main grid */}
      <Box
        sx={{
          display: "grid",
          gap: 2,
          alignItems: "start",
          gridTemplateColumns: {
            xs: "1fr",
            md: `${LAYOUT.MAP_W.md}px 1.2fr 1.6fr`,
            lg: `${LAYOUT.MAP_W.lg}px 1.2fr 1.6fr`,
          },
          gridTemplateAreas: {
            xs: areasXS,
            md: areasMD,
          },
        }}
      >
        {/* left: map spans rows */}
        <Box sx={{ gridArea: "map", minWidth: 0, position: "relative" }}>
          <Box sx={{ height: LAYOUT.MAP_H, borderRadius: 3, overflow: "hidden", border: "1px solid #262b36" }}>
            <LeafletMap
              key={stateId}
              center={[39.5, -98.35]}
              zoom={6}
              initialBounds={initialBounds}
              resizeSignal={stateId}
            >
              {isDetailed(stateId) ? (
                <DetailedCountyLayer
                  stateFips={stateId}
                  source="/us-counties.json"
                  dataMap={
                    showChoropleth
                      ? (registeredMode ? regMap : activeMode ? actMap : e1Map)
                      : undefined
                  }
                  bins={7}
                  onFeatureClick={(feature) => {
                    const geoid = String(feature?.properties?.GEOID);
                    setSelectedCounty(geoid);
                  }}
                />
              ) : (
                <SelectedStateLayer stateId={stateId} />
              )}
            </LeafletMap>
          </Box>
          {showChoropleth && (
            <ChoroplethLegend
              domain={registeredMode ? regDomain : activeMode ? actDomain : e1Domain}
              colors={registeredMode ? regColors : activeMode ? actColors : e1Colors}
              format={registeredMode ? d3.format(".0%") : activeMode ? d3.format(".0%") : d3.format(",")}
            />
          )}
        </Box>

        {/* top-middle: bar (or WA voter list) */}
        <Box sx={{ gridArea: "bar", minWidth: 0 }}>
          {registeredMode && isWA ? (
            <RegisteredVotersTable
              data={data}
              stateId={stateId}
              countyGEOID={selectedCounty}
            />
          ) : activeMode ? (
            <ActiveVotersBar data={data} stateId={stateId} />
          ) : (
            <ProvisionalBar data={data} stateId={stateId} />
          )}
        </Box>

        {/* top-right: table */}
        <Box sx={{ gridArea: "table", minWidth: 0 }}>
          {registeredMode ? (
            typeof RegistrationTable === "function" ? (
              <RegistrationTable data={data} stateId={stateId} />
            ) : (
              <Card title="Registered Voters by EAVS region (WA)">
                <div className="text-xs text-neutral-400 px-1 py-2">
                  Add <code>RegistrationTable</code> to render this table.
                </div>
              </Card>
            )
          ) : activeMode ? (
            <ActiveVotersTable data={data} stateId={stateId} />
          ) : (
            <ProvisionalTable data={data} stateId={stateId} />
          )}
        </Box>

        {/* middle row on md+: Bubble (col 2) | Equipment (col 3) */}
        {showDropBox && (
          <Box sx={{ gridArea: "bubble", minWidth: 0 }}>
            {typeof DropBoxBubbleChart === "function" ? (
              <DropBoxBubbleChart data={data} stateId={stateId} height={200} />
            ) : (
              <Card title="Drop-box Voting">
                <div className="text-xs text-neutral-400 px-1 py-2">
                  Add <code>DropBoxBubbleChart</code> to render this chart.
                </div>
              </Card>
            )}
          </Box>
        )}

        {/* bottom/right: equipment summary */}
        <Box sx={{ gridArea: "equip", minWidth: 0 }}>
          <EquipmentSummaryTable data={data} stateId={stateId} />
        </Box>
      </Box>
    </Box>
  );
}



//----- CountyView ------------------------------------------------------------
function DetailedCountyLayer({
  stateFips,
  source = "/us-counties.json",
  dataMap,                    // Map<GEOID -> number> or undefined
  bins = 5,
  color = d3.interpolateBlues,
  baseStyle = {
    weight: 0.8,
    color: "#94A3B8",         // light border
    fillColor: "#1b2230",     // <- unused by fill decision; keep tame
    fillOpacity: 0.85,
  },
  onFeatureClick,
}) {
  const [gj, setGj] = React.useState(CACHE_US_COUNTIES);
  const map = useMap();
  const layerRef = React.useRef(null);

  // load once (and cache)
  React.useEffect(() => {
    if (CACHE_US_COUNTIES) return;
    // fetch(source)
    axios.get("http://localhost:8080/api/json/us-counties")
      .then(res => { CACHE_US_COUNTIES = res.data; setGj(res.data); })
      .catch(e => console.error("Failed to load counties:", e));
  }, [source]);

  const feats = React.useMemo(() => {
    const features = gj?.features || [];
    return features.filter(f => String(f?.properties?.STATEFP) === String(stateFips));
  }, [gj, stateFips]);

  // fit to bounds on load
  React.useEffect(() => {
    if (!feats?.length) return;
    const tmp = L.geoJSON({ type: "FeatureCollection", features: feats });
    try { map.fitBounds(tmp.getBounds(), { padding: [20, 20] }); } catch {}
  }, [feats, map]);

  // palette & scale
  const palette = React.useMemo(
    () => d3.range(bins).map(i => color((i + 1) / bins)),
    [bins, color]
  );

  const scale = React.useMemo(() => {
    if (!(dataMap instanceof Map) || dataMap.size === 0) return null;
    const vals = feats
      .map(f => dataMap.get(String(f?.properties?.GEOID)))
      .filter(v => v != null);
    if (!vals.length) return null;
    return d3.scaleQuantize().domain(d3.extent(vals)).range(palette);
  }, [dataMap, feats, palette]);

  // single source of truth for fill
  const NO_DATA_FILL = "#2b3240"; // neutral/dim for dark theme
  const styleFn = React.useCallback((feature) => {
    const geoid = String(feature.properties?.GEOID);
    const v = dataMap?.get(geoid);
    const fillColor =
      scale && v != null
        ? scale(v)
        : NO_DATA_FILL;

    return { ...baseStyle, fillColor };
  }, [baseStyle, dataMap, scale]);

  // re-apply style when scale/data changes
  React.useEffect(() => {
    const layer = layerRef.current;
    if (layer && typeof layer.setStyle === "function") {
      layer.setStyle(styleFn);
    }
  }, [styleFn]);

  if (!feats?.length) return null;

function onEachFeature(feature, layer) {
  const name  = feature.properties?.NAME ?? feature.properties?.GEOID;

  // Tooltip: show the county name only
  layer.bindTooltip(name, { sticky: true, direction: "top" });

  layer.on({
    click: () => onFeatureClick && onFeatureClick(feature),

    // Emphasize border on hover, but DO NOT change fill color
    mouseover: (e) => {
      e.target.setStyle({
        ...styleFn(feature),      // keeps original computed fill (including "no data" gray)
        weight: 1.5,
        color: "#E2E8F0"
      });
      e.target.bringToFront();
    },

    // Restore the exact style computed for this feature
    mouseout: (e) => {
      e.target.setStyle(styleFn(feature));
    },
  });
}


  return (
    <GeoJSON
      ref={layerRef}
      data={{ type: "FeatureCollection", features: feats }}
      style={styleFn}
      onEachFeature={onEachFeature}
    />
  );
}


function InvalidateOnResize({ resizeSignal }) {
  const map = useMap();
  React.useEffect(() => {
    // Wait for the CSS transition to finish, then recalc
    const id = setTimeout(() => map.invalidateSize(), 320);
    return () => clearTimeout(id);
  }, [resizeSignal, map]);
  React.useEffect(()=>{
    const id = setTimeout(()=>map.invalidateSize(), 0);
    return () => clearTimeout(id);
  },[map])
  return null;
}

// ---- Leaflet Map Wrapper ---------------------------------------------------
function LeafletMap({ center, zoom, children, initialBounds, disabled = false, resizeSignal }) {
  return (
    <MapContainer 
    center={center} 
    zoom={zoom} 
    zoomSnap={0.1} 
    zoomDelta={0.5} 
    style={{height: '100%', width: '100%', background: "#F4F3EE"}}
    className={disabled?"pointer-events-none": ""}
    attributionControl ={false} 
    preferCanvas = {true} 
    zoomControl={true} 
    scrollWheelZoom = {true} 
    dragging = {true}
    whenCreated = {(m)=> setTimeout(() => m.invalidateSize(), 0)}
    >
      <InvalidateOnResize resizeSignal = {resizeSignal}/>
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
    // fetch("/us-states.json")
    axios.get("http://localhost:8080/api/json/us-states")
      .then((res) => setGeojson(res.data))
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
      style={style}
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
      .attr("opacity", 0.9)
      .append("title")
      .text((d) => d._title ? `${d._title}: ${d[yKey]}` : `${d[xKey]}: ${d[yKey]}`);
  }, [data, height, margin, width, xKey, yKey]);

  return <div ref={ref} className="w-full text-indigo-400" />;
}

// ---- D3: BubbleChart -------------------------------------------------------
function BubbleChart({
  data,
  xKey,
  yKey,
  rKey,
  colorKey,
  height = 240,
  margin = { top: 12, right: 12, bottom: 32, left: 44 },
  xDomain,
  yDomain,
  xTickFormat = (d) => `${d}%`,
  yTickFormat = (d) => `${d}%`,
}) {
  const ref = React.useRef(null);
  const [width, setWidth] = React.useState(0);

  React.useEffect(() => {
    const ro = new ResizeObserver((entries) => {
      for (const e of entries) setWidth(e.contentRect.width);
    });
    if (ref.current) ro.observe(ref.current);
    return () => ro.disconnect();
  }, []);

  React.useEffect(() => {
    const el = ref.current;
    if (!el || !width) return;

    d3.select(el).selectAll("*").remove();

    const svg = d3.select(el).append("svg").attr("width", width).attr("height", height);
    const innerW = width - margin.left - margin.right;
    const innerH = height - margin.top - margin.bottom;

    const g = svg.append("g").attr("transform", `translate(${margin.left},${margin.top})`);

    // --- robust domains -----------------------------------------------------
    function paddedDomain(values, fallback = [0, 100]) {
      if (!values?.length) return fallback;
      let [lo, hi] = d3.extent(values);
      if (lo == null || hi == null) return fallback;

      lo = Math.max(0, Math.min(100, lo));
      hi = Math.max(0, Math.min(100, hi));

      if (lo === hi) {
        const pad = hi === 0 ? 5 : Math.max(2, hi * 0.1);
        lo = Math.max(0, lo - pad);
        hi = Math.min(100, hi + pad);
      }
      return [lo, hi];
    }

    const xs = data.map(d => +d[xKey]);
    const ys = data.map(d => +d[yKey]);
    const rs = data.map(d => +d[rKey]);

    const x = d3.scaleLinear()
      .domain(xDomain ?? paddedDomain(xs))
      .nice()
      .range([0, innerW]);

    const y = d3.scaleLinear()
      .domain(yDomain ?? paddedDomain(ys))
      .nice()
      .range([innerH, 0]);

    const r = d3.scaleSqrt()
      .domain([0, d3.max(rs) || 1])
      .range([4, 18]);

    const color = d3.scaleOrdinal().domain(["R", "D"]).range(["#ef4444", "#3b82f6"]);

    // axes
    g.append("g")
      .attr("transform", `translate(0,${innerH})`)
      .call(d3.axisBottom(x).ticks(6).tickFormat(xTickFormat));

    g.append("g")
      .call(d3.axisLeft(y).ticks(5).tickFormat(yTickFormat));

    // dots
    const nodes = g.selectAll("circle.dot")
      .data(data)
      .join("circle")
      .attr("class", "dot")
      .attr("cx", d => x(+d[xKey]))
      .attr("cy", d => y(+d[yKey]))
      .attr("r",  d => r(+d[rKey]))
      .attr("fill", d => color(d[colorKey]))
      .attr("fill-opacity", 0.85)
      .attr("stroke", "#111")
      .attr("stroke-width", 0.6);

    nodes.append("title").text(d =>
      `${d.label ?? ""}\n% Rep: ${xTickFormat(+d[xKey])}\n% Drop-box: ${yTickFormat(+d[yKey])}\nDrop-box votes: ${d3.format(",")(d[rKey])}`
    );
  }, [data, xKey, yKey, rKey, colorKey, height, margin, width, xDomain, yDomain, xTickFormat, yTickFormat]);

  return <div ref={ref} className="w-full" />;
}

// ---- UI helpers ------------------------------------------------------------
function Card({ title, children }) {
  return (
    <div className="rounded-2xl border border-neutral-800 bg-neutral-900 p-3 shadow-lg">
      <div className="mb-2 flex items-center justify-between">
        <h3 className="text-sm font-semibold tracking-tight text-neutral-200">{title}</h3>
      </div>
      {children}
    </div>
  );
}
