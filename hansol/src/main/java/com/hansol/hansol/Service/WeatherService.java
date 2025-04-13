package com.hansol.hansol.Service;

import com.hansol.hansol.Dto.WeatherDto;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


@Service
public class WeatherService {

    private RestTemplate restTemplate = new RestTemplate();
    private final String apiUrl = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";
    private final String ServiceKey = "UGVc41C%2B%2FcUvUxumr3aNPb%2FdVTiFatzrAS99ZkHYRxUSVoedG2IKA7gTwCI7hr0kRXSQJd%2FBNmTCQOVE87Fyeg%3D%3D";

//    기상청 api 요청
    public String getWeatherData() {
//        기상청에서 3시간 간격으로 예보하는 시간
        int[] frcstTimes = {2,5,8,11,14,17,17,20,23};

        StringBuilder urlBuilder = new StringBuilder(apiUrl);
        String pageNo = "1";
        String numOfRows = "1000";
        String dataType = "JSON";
        String base_date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String base_time = "";

        try{
//            가장 근접한 예보시간 설정
            int time = LocalTime.now().getHour();
            int frcstTime = 2;

            for(int i=frcstTimes.length-1; i>=0; i--){
                if(time>=frcstTimes[i]){
                    frcstTime = frcstTimes[i];
                    break;
                }
            }

            base_time = Integer.toString(frcstTime) + "10";

            urlBuilder.append("?ServiceKey=").append(ServiceKey)
                    .append("&numOfRows=").append(numOfRows)
                    .append("&pageNo=").append(pageNo)
                    .append("&dataType=").append(dataType)
                    .append("&base_date=").append(base_date)
                    .append("&base_time=").append(base_time)
                    .append("&nx=").append("60")
                    .append("&ny=").append("127");

            String url = urlBuilder.toString();

            URI uri = new URI(url);

            System.out.println(String.format("URI : %s, Time : %s", uri, base_time));

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return response.getBody();
        } catch(Exception e){
            e.printStackTrace();
            return "Error : " + e.getMessage();
        }
    }

//    DTO생성 후 컨트롤러로 넘김
    @Scheduled(fixedRate = 1800000) //30분마다 갱신
    public WeatherDto getWeather(){
        String jsonData = getWeatherData();
        String base_date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String base_time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH")) + "00";

        try{
//            JSON응답 파싱
            JSONObject jsonResponse = new JSONObject(jsonData);
            JSONArray items = jsonResponse.getJSONObject("response")
                    .getJSONObject("body")
                    .getJSONObject("items")
                    .getJSONArray("item");

            String temperature = "";
            String tMax = "";
            String tMin = "";
            String rain = "";
            String weatherType = "";

            for(int i=0; i<items.length(); i++){
                JSONObject item = items.getJSONObject(i);
                String fcstDate = item.getString("fcstDate");
                String fcstTime = item.getString("fcstTime");


                //당일 날씨예보만 저장
                if(fcstDate.equals(base_date) && fcstTime.equals(base_time)){
                    String category = item.getString("category");
                    String fcstValue = item.getString("fcstValue");

                    //category별 값 저장
                    switch (category){
                        case "TMP":
                            temperature =  fcstValue + "℃";
                            break;
                        case "POP":
                            rain = fcstValue + "%";
                            break;
//                        case "TMN":
//                            tMin = fcstValue + "℃";
//                            break;
//                        case "TMX":
//                            tMax = fcstValue + "℃";
//                            break;
                        case "SKY":
                            weatherType = fcstValue;
                            if(weatherType.equals("1")){
                                weatherType = "☀\uFE0F 맑음";
                            } else if(weatherType.equals("3")){
                                weatherType = "☁\uFE0F 구름 많음";
                            } else{
                                weatherType = "\uD83C\uDF25\uFE0F 흐림";
                            }
                            break;
                    }
                }
            }

            return new WeatherDto(temperature, rain, weatherType);
        } catch(Exception e){
            e.printStackTrace();
            return new WeatherDto("N/A", "N/A", "데이터 오류");
        }
    }
}
