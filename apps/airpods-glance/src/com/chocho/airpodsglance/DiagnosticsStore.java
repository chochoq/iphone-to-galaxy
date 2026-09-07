package com.chocho.airpodsglance;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class DiagnosticsStore {
    private static final String PREFS = "airpods_diagnostics";
    private final SharedPreferences prefs;

    public DiagnosticsStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void increment(String key) {
        synchronized (DiagnosticsStore.class) {
            prefs.edit().putLong(key, prefs.getLong(key, 0L) + 1L).commit();
        }
    }

    public void addAll(Map<String, Long> deltas) {
        if (deltas == null || deltas.isEmpty()) return;
        synchronized (DiagnosticsStore.class) {
            SharedPreferences.Editor editor = prefs.edit();
            for (Map.Entry<String, Long> entry : deltas.entrySet()) {
                editor.putLong(entry.getKey(), prefs.getLong(entry.getKey(), 0L) + entry.getValue());
            }
            editor.commit();
        }
    }

    public void setLastScanFailure(int code) {
        prefs.edit().putInt("last_scan_failure", code).apply();
    }

    public String summary() {
        return "Apple 후보 " + value("filtered")
                + " · 유효 " + value("valid")
                + " · 형식 거부 " + value("rejected")
                + " · 주소 불일치 " + value("address_mismatch")
                + " · 스캔 실패 " + value("scan_failed")
                + "\n선택 주소와 같은 Apple 신호 " + value("apple_exact_address")
                + " · 다른 주소 " + value("apple_other_address")
                + "\n거부 상세 — 길이 " + value("rejected_too_short")
                + " · 종류 " + value("rejected_wrong_type")
                + " · 선언 길이 " + value("rejected_wrong_declared_length")
                + " · prefix " + value("rejected_wrong_prefix")
                + "\n관측된 Apple 종류 " + typeSummary()
                + "\n직접 연결 — API " + value("aap_public_api_ready")
                + " · 자동 " + value("aap_automatic_started")
                + " · 수동 " + value("aap_manual_started")
                + " · 소켓 생성 " + value("aap_socket_created")
                + " · 연결 " + value("aap_socket_connected")
                + " · 인사 전송 " + value("aap_handshake_sent")
                + " · 응답 " + value("aap_handshake_ok")
                + " · 활성화 " + value("aap_post_handshake_v2_sent")
                + " · 배터리 " + value("aap_battery_received")
                + " · 시간초과 " + (value("aap_connect_timeout")
                    + value("aap_handshake_timeout"))
                + " · 실패 " + value("aap_failed")
                + "\n소켓 세부 — class " + value("aap_socket_classes_loaded")
                + " · builder " + value("aap_socket_builder_created")
                + " · type " + value("aap_socket_type_set")
                + " · PSM " + value("aap_socket_psm_set")
                + " · 보안 " + value("aap_socket_security_set")
                + " · build " + value("aap_socket_settings_built")
                + " · method " + value("aap_socket_method_found")
                + " · invoke " + value("aap_socket_invoked")
                + "\n실험 fallback — 시작 " + value("aap_hidden_fallback_started")
                + " · exemption " + value("aap_hidden_exemption_ready")
                + " · method " + value("aap_hidden_method_found")
                + " · invoke " + value("aap_hidden_socket_invoked")
                + " · 창 종료 " + value("aap_direct_window_closed")
                + "\n수납 신호 실험(누적·좌우 미확정) — 보고 " + value("aap_placement_received")
                + " · 미지원 길이 " + value("aap_placement_unsupported_length")
                + " · 미상 포함 " + value("aap_placement_unknown")
                + " · 02/02 " + value("aap_placement_slots_2_2")
                + "\n연결 카드 — 크게 " + value("popup_overlay_shown")
                + " · 알림 대체 " + value("popup_notification_fallback");
    }

    public long value(String key) { return prefs.getLong(key, 0L); }

    private String typeSummary() {
        List<Map.Entry<String, ?>> types = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("type_") && entry.getValue() instanceof Long) {
                types.add(entry);
            }
        }
        Collections.sort(types, new Comparator<Map.Entry<String, ?>>() {
            @Override public int compare(Map.Entry<String, ?> a, Map.Entry<String, ?> b) {
                return Long.compare((Long) b.getValue(), (Long) a.getValue());
            }
        });
        if (types.isEmpty()) return "아직 없음";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < types.size() && i < 5; i++) {
            if (result.length() > 0) result.append(" · ");
            Map.Entry<String, ?> entry = types.get(i);
            result.append("0x").append(entry.getKey().substring(5).toUpperCase())
                    .append(' ').append(entry.getValue());
        }
        return result.toString();
    }
}
