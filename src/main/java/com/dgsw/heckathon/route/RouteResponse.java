package com.dgsw.heckathon.route;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private List<Waypoint> waypoints;
    private String message; // Optional: 응답 메시지 추가
}