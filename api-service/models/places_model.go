package models

type PlacesResponse struct {
	Results []struct {
		Name             string   `json:"name"`
		Address          string   `json:"vicinity"`
		Rating           float64  `json:"rating,omitempty"`
		UserRatingsTotal int      `json:"user_ratings_total,omitempty"`
		PlaceID          string   `json:"place_id"`
		Types            []string `json:"types,omitempty"`
		Geometry         struct {
			Location struct {
				Lat float64 `json:"lat"`
				Lng float64 `json:"lng"`
			} `json:"location"`
		} `json:"geometry"`
		OpeningHours struct {
			OpenNow bool `json:"open_now,omitempty"`
		} `json:"opening_hours,omitempty"`
		Photos []struct {
			PhotoReference string `json:"photo_reference"`
			Height         int    `json:"height"`
			Width          int    `json:"width"`
		} `json:"photos,omitempty"`
		PhotoURLs        []string `json:"photo_urls,omitempty"`
	} `json:"results"`
	Status string `json:"status"`
}
