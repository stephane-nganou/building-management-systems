package com.bms.apartment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Room breakdown of an apartment. */
@Embeddable
public class RoomLayout {

    @Column(name = "rooms", nullable = false)
    private int rooms;

    @Column(name = "bedrooms", nullable = false)
    private int bedrooms;

    @Column(name = "bathrooms", nullable = false)
    private int bathrooms;

    @Column(name = "kitchens", nullable = false)
    private int kitchens;

    @Column(name = "toilets", nullable = false)
    private int toilets;

    protected RoomLayout() {
        // for JPA
    }

    public RoomLayout(int rooms, int bedrooms, int bathrooms, int kitchens, int toilets) {
        this.rooms = rooms;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.kitchens = kitchens;
        this.toilets = toilets;
    }

    public int getRooms() {
        return rooms;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public int getBathrooms() {
        return bathrooms;
    }

    public int getKitchens() {
        return kitchens;
    }

    public int getToilets() {
        return toilets;
    }
}
