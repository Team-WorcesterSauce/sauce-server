package com.dgsw.heckathon.route;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {
    private double startLat;
    private double startLon;
    private double endLat;
    private double endLon;
}
