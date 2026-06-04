import React from 'react';

/**
 * A simple event log panel. Pair it with a `log` state array and an `emit` helper:
 *
 *   const [log, setLog] = useState([]);
 *   const emit = (msg) => setLog((prev) => [msg, ...prev].slice(0, 8));
 */
export function EventLog({ log, label = 'interact with the table' }) {
    return (
        <div style={{ flex: '0 0 360px', fontFamily: 'monospace', fontSize: 14, color: '#555' }}>
            <strong>Event log</strong>
            {label && <div style={{ color: '#aaa', marginBottom: 4 }}>{label}</div>}
            {log.length === 0
                ? <div style={{ color: '#aaa' }}>no events yet</div>
                : log.map((entry, i) => <div key={i}>{entry}</div>)
            }
        </div>
    );
}
