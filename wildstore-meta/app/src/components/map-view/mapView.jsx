import { useState, useMemo, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polygon, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useSelector, useDispatch } from 'react-redux';
import { addQuery, setCurrentPage } from '../../redux/filterSlice';

import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import iconUrl from 'leaflet/dist/images/marker-icon.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';

L.Icon.Default.mergeOptions({
  iconRetinaUrl,
  iconUrl,
  shadowUrl,
});

const EXTENT_THRESHOLD = 0.02; // If bbox smaller than this, treat as point

/**
 * Component to handle map interactions (Search Button)
 * We need this child component to access the 'map' instance via useMap()
 */
const MapController = () => {
  const map = useMap();
  const dispatch = useDispatch();

  useEffect(() => {
    const handleSearch = () => {
      const bounds = map.getBounds();
      const ne = bounds.getNorthEast();
      const sw = bounds.getSouthWest();

      const query = `LOCATION IN ((${ne.lat}, ${sw.lng}), (${ne.lat},${ne.lng}), (${sw.lat},${ne.lng}), (${sw.lat}, ${sw.lng}))`;

      dispatch(addQuery(query));
      dispatch(setCurrentPage(1));
    };
    const SearchControl = L.Control.extend({
      onAdd: () => {
        const button = L.DomUtil.create('button', 'leaflet-bar leaflet-control leaflet-control-custom');
        button.innerHTML = 'Search this area';
        button.style.backgroundColor = 'white';
        button.style.color = '#0056b3';
        button.style.padding = '10px 15px';
        button.style.border = '2px solid rgba(0,0,0,0.2)';
        button.style.borderRadius = '20px';
        button.style.cursor = 'pointer';
        button.style.fontSize = '14px';
        button.style.fontWeight = 'bold';
        button.style.boxShadow = '0 1px 5px rgba(0,0,0,0.65)';
        
        button.onclick = (e) => {
          L.DomEvent.stopPropagation(e); // Prevent map click
          handleSearch();
        };
        return button;
      }
    });

    const searchControl = new SearchControl({ position: 'topcenter' });
    searchControl.setPosition('topright'); 
    map.addControl(searchControl);

    return () => {
      map.removeControl(searchControl);
    };
  }, [map, dispatch]);

  return null;
};

const MapView = () => {
  const [selectedId, setSelectedId] = useState(null); // Which square has its popup open

  const metadataRecords = useSelector(
    (state) => state.metadataReducer.metadataRecords
  );

  // Build features based on min/max latitude/longitude
  const features = useMemo(() => {
    if (!metadataRecords || metadataRecords.length === 0) return [];

    return metadataRecords
      .map((record, idx) => {
        const vars = record.variables || [];

        const latVar =
          vars.find(
            (v) =>
              v.variableName === 'latitude' ||
              v.variableName === 'XLAT'
          ) || null;

        const lonVar =
          vars.find(
            (v) =>
              v.variableName === 'longitude' ||
              v.variableName === 'XLONG'
          ) || null;

        // If we have no lat/lon info, we can’t place it
        if (!latVar || !lonVar) return null;

        const latMin = parseFloat(latVar.minValue);
        const latMax = parseFloat(latVar.maxValue);
        const lonMin = parseFloat(lonVar.minValue);
        const lonMax = parseFloat(lonVar.maxValue);

        if (
          !isFinite(latMin) ||
          !isFinite(latMax) ||
          !isFinite(lonMin) ||
          !isFinite(lonMax)
        ) {
          return null;
        }

        const centerLat = (latMin + latMax) / 2;
        const centerLng = (lonMin + lonMax) / 2;

        const latSpan = Math.abs(latMax - latMin);
        const lonSpan = Math.abs(lonMax - lonMin);

        const id = record.digestString || idx;

        // If bbox big enough, draw a square
        // If too small, treat as point and only show a pin.
        if (latSpan >= EXTENT_THRESHOLD && lonSpan >= EXTENT_THRESHOLD) {
          const polygonLatLngs = [
            [latMin, lonMax],
            [latMax, lonMax],
            [latMax, lonMin],
            [latMin, lonMin],
            [latMin, lonMax],
          ];

          return {
            id,
            record,
            type: 'polygon',
            polygonLatLngs,
            markerPos: [centerLat, centerLng], 
          };
        } else {
          return {
            id,
            record,
            type: 'point',
            polygonLatLngs: null,
            markerPos: [centerLat, centerLng],
          };
        }
      })
      .filter(Boolean);
  }, [metadataRecords]);

  const renderPopupContent = (record) => (
    <div style={{ fontSize: '12px', fontFamily: 'monospace' }}>
      <h3
        style={{
          margin: '0 0 8px 0',
          color: '#0056b3',
          fontSize: '14px',
          wordBreak: 'break-all',
        }}
      >
        📄 {record.fileName?.[0] || 'Dataset'}
      </h3>

      <div
        style={{
          borderBottom: '1px solid #eee',
          paddingBottom: '5px',
          marginBottom: '5px',
        }}
      >
        <strong>Domain:</strong> {record.domain ?? 'N/A'}
        <br />
        <strong>Size:</strong> {record.fileSize ?? 'N/A'}
        <br />
        <strong>Path:</strong> {record.filePath?.[0] || '(unknown)'}
      </div>

      <div
        style={{
          backgroundColor: '#f9f9f9',
          padding: '5px',
          borderRadius: '4px',
        }}
      >
        <strong>Variables:</strong>
        <ul
          style={{
            margin: '4px 0',
            paddingLeft: '20px',
            color: '#444',
            listStyleType: 'disc',
          }}
        >
          {(record.variables || []).slice(0, 12).map((v) => {
            const attrs = v.attributeList || [];
            const dims = (v.varDimensionList || [])
              .map((d) => `${d.name}=${d.value}`)
              .join(', ');

            const unitsAttr = attrs.find((a) => a.attributeName === 'units');
            const units = Array.isArray(unitsAttr?.value)
              ? unitsAttr.value[0]
              : unitsAttr?.value;

            const min =
              typeof v.minValue === 'number'
                ? v.minValue
                : parseFloat(v.minValue);
            const max =
              typeof v.maxValue === 'number'
                ? v.maxValue
                : parseFloat(v.maxValue);

            const hasRange = Number.isFinite(min) && Number.isFinite(max);

            return (
              <li key={v.variableName}>
                <strong>{v.variableName}</strong>
                {dims && (
                  <span style={{ color: '#666' }}> &nbsp;({dims})</span>
                )}
                <br />
                <span style={{ color: '#666', fontSize: '11px' }}>
                  {v.type && `type: ${v.type}`}
                  {units && ` • units: ${units}`}
                  {hasRange &&
                    ` • range: ${min.toFixed(2)} – ${max.toFixed(2)}`}
                </span>
              </li>
            );
          })}

          {record.variables && record.variables.length > 12 && (
            <li>
              ... (+{record.variables.length - 12} more)
            </li>
          )}
        </ul>
      </div>
    </div>
  );

  return (
    <div style={{ position: 'relative', height: '100vh', width: '100%' }}>
      <MapContainer
        center={[0, 0]}
        zoom={2}
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />

        {features.map((feat) => {
          if (feat.type === 'polygon') {
            return (
              <Polygon
                key={feat.id}
                positions={feat.polygonLatLngs}
                pathOptions={{
                  color: selectedId === feat.id ? 'red' : 'gray',
                  weight: selectedId === feat.id ? 3 : 1,
                  fillOpacity: 0.2,
                }}
                eventHandlers={{
                  click: () => setSelectedId(feat.id),
                }}
              >
                <Popup
                  eventHandlers={{
                    remove: () => setSelectedId(null),
                  }}
                >
                  {renderPopupContent(feat.record)}
                </Popup>
              </Polygon>
            );
          }

          // point-only fallback (no usable bbox)
          return (
            <Marker key={feat.id} position={feat.markerPos}>
              <Popup>{renderPopupContent(feat.record)}</Popup>
            </Marker>
          );
        })}
        <MapController />
      </MapContainer>
    </div>
  );
};

export default MapView;
