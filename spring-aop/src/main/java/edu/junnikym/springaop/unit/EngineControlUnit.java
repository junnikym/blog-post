package edu.junnikym.springaop.unit;

import org.springframework.stereotype.Service;

@Service
public class EngineControlUnit implements Unit {

    private static String name = "engine control unit";

    private static String description = "An engine control unit (ECU), also called an engine control module (ECM)" +
            ", is a device which controls multiple systems of an internal combustion engine in a single unit. " +
            "Systems commonly controlled by an ECU include the fuel injection and ignition systems.";

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

}
