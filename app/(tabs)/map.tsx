import { useRouter } from "expo-router";
import React, { useCallback } from "react";
import { View } from "react-native";
import { WebView, WebViewMessageEvent } from "react-native-webview";
import { MOCK_POSTS } from "../../src/lib/data/feedMock";

const JS_KEY = "aabddec83fa66a4258fb57e94b4d61e1"; // 백엔드가 준 JS 키

// 지도에 보낼 데이터(말풍선: 제목/요약만 필요)
const mapData = MOCK_POSTS.filter(
  (p) =>
    p.coords &&
    typeof p.coords.lat === "number" &&
    typeof p.coords.lng === "number"
).map((p) => ({
  id: p.id,
  type: p.type,
  title: p.title,
  author: p.author,
  avatarUrl: p.avatarUrl || "",
  location: p.location ?? "",
  categories: p.categories ?? [],
  capacity: p.capacity ?? null,
  joined: p.joined ?? null,
  volunteerDate: p.volunteerDate ?? "",
  lat: p.coords!.lat,
  lng: p.coords!.lng,
}));

const injectedJson = JSON.stringify(mapData);

const html = `
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="initial-scale=1, width=device-width, user-scalable=no"/>
  <style>
    html, body { height:100%; }
    body { margin:0; padding:0; background:#EAEAEA; font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Apple SD Gothic Neo","Noto Sans KR","Helvetica Neue",Arial,"Malgun Gothic",sans-serif; }
    #map { width:100vw; height:100vh; }
    .shadow { box-shadow: 0 6px 20px rgba(0,0,0,0.12); }

    /* — 최소 팝업: 제목 + 모집 요약 + 버튼 — */
    .mini-card {
      max-width: 320px;
      background:#fff;
      border-radius:12px;
      border:1px solid #E5E5E5;
      padding:10px 12px;
      position:relative;
    }
    .row { display:flex; align-items:center; }
    .avatar { width:28px; height:28px; border-radius:14px; object-fit:cover; background:#f5f5f5; }
    .title { font-weight:800; font-size:15px; color:#333; margin-left:8px; flex:1; }
    .badge { margin-left:6px; font-size:11px; padding:2px 6px; border-radius:8px; background:#FF8A00; color:#fff; font-weight:700; }
    .badge.review { background:#0B63B6; }

    .summary-box { background:#F5F5F5; padding:10px; border-radius:12px; margin-top:10px; }
    .summary-header { display:flex; align-items:center; gap:8px; }
    .summary-title { font-weight:800; color:#111; font-size:14px; }
    .summary-line { display:flex; align-items:center; margin:4px 0 6px; color:#666; font-size:12.5px; }
    .map-pin { width:16px; height:16px; vertical-align:-2px; margin-right:6px; }
    .chips-row { display:flex; flex-wrap:wrap; gap:6px; margin-top:6px; }
    .chip { background:#EFEFEF; padding:5px 8px; border-radius:12px; font-weight:700; color:#555; font-size:11.5px; display:inline-block; }

    .cta { margin-top:10px; width:100%; padding:9px 10px; border-radius:10px; border:1px solid #FF8A00; background:#FF8A00; color:#fff; font-weight:800; font-size:13px; text-align:center; cursor:pointer; }
    .close-x { position:absolute; top:6px; right:8px; font-size:16px; color:#999; cursor:pointer; }
  </style>
</head>
<body>
  <div id="map"></div>

  <!-- autoload=false: 우리가 준비되면 명시적으로 load 호출 -->
  <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${JS_KEY}&autoload=false&libraries=services,clusterer"></script>
  <script>
    const POSTS = ${injectedJson};

    // 안전한 preventMap 호출 래퍼
    function safePreventMap() {
      try {
        if (window.kakao && kakao.maps && kakao.maps.event && kakao.maps.event.preventMap) {
          kakao.maps.event.preventMap();
        }
      } catch (e) {}
    }

    function initMap() {
      const defaultCenter = new kakao.maps.LatLng(37.5665,126.9780);
      const center = POSTS.length ? new kakao.maps.LatLng(POSTS[0].lat, POSTS[0].lng) : defaultCenter;

      const container = document.getElementById('map');
      if (!container) return;

      const map = new kakao.maps.Map(container, { center, level:6 });

      const iconSVG = \`
        <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" fill="orange" stroke="white" stroke-width="1.5" viewBox="0 0 24 24">
          <path d="M12 22s8-7.58 8-12a8 8 0 1 0-16 0c0 4.42 8 12 8 12z"/>
          <circle cx="12" cy="10" r="3"/>
          <line x1="9" y1="10" x2="15" y2="10" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
        </svg>\`;
      const iconURL = 'data:image/svg+xml;base64,' + btoa(iconSVG);
      const markerImage = new kakao.maps.MarkerImage(iconURL, new kakao.maps.Size(54,54));

      let currentOverlay = null;
      kakao.maps.event.addListener(map, 'click', function(){
        if (currentOverlay) { currentOverlay.setMap(null); currentOverlay = null; }
      });

      function createMiniOverlay(item){
        const root = document.createElement('div');
        root.className = 'shadow';
        root.style.position = 'relative';
        root.style.transform = 'translateY(-8px)';

        const card = document.createElement('div');
        card.className = 'mini-card';

        // 카드 전체에서 맵으로 이벤트 전파 방지
        const stopAll = (e) => {
          if (e && e.stopPropagation) e.stopPropagation();
          safePreventMap();
        };
        card.addEventListener('click', stopAll);
        card.addEventListener('touchstart', stopAll, { passive: false });
        card.addEventListener('touchend', stopAll, { passive: false });

        const closeX = document.createElement('div');
        closeX.className = 'close-x';
        closeX.textContent = '×';
        closeX.onclick = function(e){
          if (e && e.stopPropagation) e.stopPropagation();
          safePreventMap();
          if (currentOverlay) { currentOverlay.setMap(null); currentOverlay = null; }
        };

        // Header (제목/배지)
        const header = document.createElement('div');
        header.className = 'row';
        header.addEventListener('click', stopAll);

        const avatar = document.createElement('img');
        avatar.className = 'avatar';
        avatar.src = item.avatarUrl || '';
        avatar.onerror = function(){ this.style.display='none'; };

        const title = document.createElement('div');
        title.className = 'title';
        title.textContent = item.title || '(제목 없음)';

        const badge = document.createElement('span');
        badge.className = 'badge' + (item.type === 'review' ? ' review' : '');
        badge.textContent = item.type === 'review' ? '후기' : '모집';

        header.appendChild(avatar);
        header.appendChild(title);
        header.appendChild(badge);

        // 모집 요약
        const summary = document.createElement('div');
        summary.className = 'summary-box';

        const sHeader = document.createElement('div');
        sHeader.className = 'summary-header';
        const sTitle = document.createElement('span');
        sTitle.className = 'summary-title';
        sTitle.textContent = '모집 요약';
        const sBadge = document.createElement('span');
        sBadge.className = 'badge';
        sBadge.textContent = '모집';
        if (item.type === 'review') sBadge.style.display = 'none';
        sHeader.appendChild(sTitle);
        sHeader.appendChild(sBadge);

        const sLine = document.createElement('div');
        sLine.className = 'summary-line';
        sLine.innerHTML = '<svg class="map-pin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="#666" stroke-width="2"><path d="M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 1 1 18 0Z"/><circle cx="12" cy="10" r="3"/></svg>' + (item.location || '');

        const chipsRow = document.createElement('div');
        chipsRow.className = 'chips-row';
        const chip = (t) => {
          const c = document.createElement('span');
          c.className = 'chip';
          c.textContent = t;
          return c;
        };
        if (item.capacity != null) chipsRow.appendChild(chip('정원: ' + item.capacity + '명'));
        if (item.joined != null) chipsRow.appendChild(chip('현재: ' + item.joined + '명'));
        (item.categories || []).forEach((c) => chipsRow.appendChild(chip(c)));
        if (item.volunteerDate) chipsRow.appendChild(chip('봉사 일시: ' + item.volunteerDate));

        summary.appendChild(sHeader);
        summary.appendChild(sLine);
        summary.appendChild(chipsRow);

        // 자세히 보기
        const btn = document.createElement('button');
        btn.className = 'cta';
        btn.textContent = '자세히 보기';
        const onOpen = function(e){
          if (e && e.stopPropagation) e.stopPropagation();
          safePreventMap();
          if (window.ReactNativeWebView && window.ReactNativeWebView.postMessage) {
            window.ReactNativeWebView.postMessage(JSON.stringify({
              type: 'openPost',
              id: item.id,
              title: item.title || ''
            }));
          }
        };
        btn.addEventListener('click', onOpen);
        btn.addEventListener('touchend', onOpen, { passive: false });

        card.appendChild(closeX);
        card.appendChild(header);
        card.appendChild(summary);
        card.appendChild(btn);

        root.appendChild(card);
        return root;
      }

      function openOverlayFor(item, position){
        if (currentOverlay) { currentOverlay.setMap(null); currentOverlay = null; }
        const contentNode = createMiniOverlay(item);
        const overlay = new kakao.maps.CustomOverlay({
          map: map,
          position: position,
          yAnchor: 1.05,
          content: contentNode
        });
        currentOverlay = overlay;
      }

      // 마커 생성
      POSTS.forEach((item) => {
        const pos = new kakao.maps.LatLng(item.lat, item.lng);
        const marker = new kakao.maps.Marker({ position: pos, image: markerImage });
        marker.setMap(map);
        kakao.maps.event.addListener(marker, 'click', function(){
          openOverlayFor(item, pos);
        });
      });

      // (선택) 기준 마커
      const cityHall = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(37.5665,126.9780),
        image: markerImage
      });
      cityHall.setMap(map);
    }

    // SDK 로드 대기: kakao.maps 존재할 때까지 체크 후 load(init)
    (function waitForKakao(){
      const maxTry = 50; // ~5초
      let count = 0;
      const timer = setInterval(function(){
        if (window.kakao && kakao.maps && kakao.maps.load) {
          clearInterval(timer);
          kakao.maps.load(initMap);
        } else if (++count >= maxTry) {
          clearInterval(timer);
          // RN으로 에러 전달
          if (window.ReactNativeWebView && window.ReactNativeWebView.postMessage) {
            window.ReactNativeWebView.postMessage(JSON.stringify({ type:'kakaoLoadError' }));
          }
        }
      }, 100);
    })();
  </script>
</body>
</html>
`;

export default function MapScreen() {
  const router = useRouter();

  const onMessage = useCallback(
    (e: WebViewMessageEvent) => {
      try {
        const data = JSON.parse(e.nativeEvent.data || "{}");
        if (data?.type === "openPost" && data?.id) {
          const id = encodeURIComponent(String(data.id));
          const title = encodeURIComponent(String(data.title || ""));
          router.push(`/(tabs)/feed?postId=${id}&q=${title}`); // 그룹명 다르면 바꿔줘!
        } else if (data?.type === "kakaoLoadError") {
          console.warn("Kakao SDK load failed in WebView");
        }
      } catch {
        // ignore
      }
    },
    [router]
  );

  return (
    <View style={{ flex: 1 }}>
      <WebView
        originWhitelist={["*"]}
        source={{ html }}
        onMessage={onMessage}
        javaScriptEnabled
        domStorageEnabled
        mixedContentMode="always"
        // 디버깅 원하면 다음 주석 해제:
        // onError={(e) => console.log("WV error", e.nativeEvent)}
        // onHttpError={(e) => console.log("WV http error", e.nativeEvent)}
      />
    </View>
  );
}
