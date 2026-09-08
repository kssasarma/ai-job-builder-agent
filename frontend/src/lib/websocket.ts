import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export interface OperationStatusMessage {
  type: string;
  status: string;
  errorMessage: string;
}

const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";
const WS_URL = apiBase.replace(/\/api\/?$/, "") + "/ws";

/**
 * Subscribes to /topic/operations/{referenceId} — the push side of AsyncOperation
 * status (resume scoring, candidate matching, profile extraction). Replaces the old
 * "poll every 3 seconds" pattern: pass null while there's nothing to watch, an id once
 * an async operation starts, and the callback fires the moment the backend pushes a
 * status change instead of waiting for the next poll tick.
 */
export function useOperationStatus(referenceId: string | null, onMessage: (payload: OperationStatusMessage) => void) {
  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  useEffect(() => {
    if (!referenceId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as unknown as WebSocket,
      reconnectDelay: 3000,
    });

    client.onStompError = (frame) => {
      console.error("WebSocket STOMP error", frame.headers["message"]);
    };

    client.onConnect = () => {
      client.subscribe(`/topic/operations/${referenceId}`, (message) => {
        try {
          onMessageRef.current(JSON.parse(message.body));
        } catch (e) {
          console.error("Failed to parse operation status push", e);
        }
      });
    };

    client.activate();
    return () => {
      client.deactivate();
    };
  }, [referenceId]);
}
