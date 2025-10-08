import { View } from "react-native";
import { WebView } from "react-native-webview";

const JS_KEY = "aabddec83fa66a4258fb57e94b4d61e1"; // 백엔드가 준 JS 키

const html = `  
<!DOCTYPE html>
<html>
<head><meta name="viewport" content="initial-scale=1, width=device-width, user-scalable=no"/></head>
<body style="margin:0;padding:0;">
  <div id="map" style="width:100vw;height:100vh;"></div>
  <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${JS_KEY}&autoload=false&libraries=services,clusterer"></script>
  <script>
    kakao.maps.load(function(){
      const container = document.getElementById('map');
      const options = {
        center: new kakao.maps.LatLng(37.5665, 126.9780), // 서울 시청
        level: 5
      };
      const map = new kakao.maps.Map(container, options);

      // 샘플 마커
      const marker = new kakao.maps.Marker({ position: new kakao.maps.LatLng(37.5665,126.9780) });
      marker.setMap(map);
    });
  </script>
</body>
</html>
`;

export default function MapScreen() {
  return (
    <View style={{ flex: 1 }}>
      <WebView originWhitelist={["*"]} source={{ html }} />
    </View>
  );
}
