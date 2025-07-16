package com.dgsw.heckathon.route;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/route")
    public ResponseEntity<RouteResponse> calculateOptimalRoute(@RequestBody RouteRequest request) {
        // 입력값 유효성 검사
        if (request.getStartLat() == 0 && request.getStartLon() == 0 &&
                request.getEndLat() == 0 && request.getEndLon() == 0) {
            return new ResponseEntity<>(new RouteResponse(null, "Invalid coordinates provided"), HttpStatus.BAD_REQUEST);
        }
        // 위도/경도 범위 검사는 Service 또는 DTO에서 더 상세하게 할 수 있습니다.

        try {
            List<Waypoint> waypoints = routeService.calculateOptimalRoute(
                    request.getStartLat(), request.getStartLon(),
                    request.getEndLat(), request.getEndLon()
            );
            return new ResponseEntity<>(new RouteResponse(waypoints, "Optimal route calculated successfully"), HttpStatus.OK);
        } catch (Exception e) {
            // 실제 운영 환경에서는 더 상세한 로깅과 에러 처리가 필요합니다.
            e.printStackTrace();
            return new ResponseEntity<>(new RouteResponse(null, "Failed to calculate optimal route: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
