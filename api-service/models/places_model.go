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

// Geometry defines the geographic coordinates.
type Geometry struct {
	Location struct {
		Lat float64 `json:"lat"`
		Lng float64 `json:"lng"`
	} `json:"location"`
}

// PlacePhoto defines the structure for individual photos as returned by Google,
// which we use to build our full URLs.
type PlacePhoto struct {
	PhotoReference  string   `json:"photo_reference"`
	Height          int      `json:"height"`
	Width           int      `json:"width"`
	HTMLAttribution []string `json:"html_attributions,omitempty"`
}

// PlaceReview defines the structure for user reviews.
type PlaceReview struct {
	AuthorName              string `json:"author_name"`
	AuthorURL               string `json:"author_url,omitempty"`
	Language                string `json:"language,omitempty"`
	ProfilePhotoURL         string `json:"profile_photo_url,omitempty"`
	Rating                  int    `json:"rating"` // Usually 1-5
	RelativeTimeDescription string `json:"relative_time_description"`
	Text                    string `json:"text"`
	Time                    int64  `json:"time"` // Unix timestamp
}

// OpeningHoursPeriodDetail defines when a place opens or closes.
type OpeningHoursPeriodDetail struct {
	Day  int    `json:"day"`  // 0-6, Sunday to Saturday
	Time string `json:"time"` // "HHMM"
}

// OpeningHoursPeriod defines a period when a place is open.
type OpeningHoursPeriod struct {
	Open  OpeningHoursPeriodDetail `json:"open"`
	Close OpeningHoursPeriodDetail `json:"close,omitempty"` // Close might not be present if open 24 hours
}

// PlaceOpeningHours defines the opening hours structure.
type PlaceOpeningHours struct {
	OpenNow           bool                 `json:"open_now,omitempty"`
	Periods           []OpeningHoursPeriod `json:"periods,omitempty"`
	WeekdayText       []string             `json:"weekday_text,omitempty"` // e.g., "Monday: 9:00 AM – 5:00 PM"
	PermanentlyClosed bool                 `json:"permanently_closed,omitempty"`
}

// PlaceDetailResult contains the detailed information for a single place.
type PlaceDetailResult struct {
	Name                     string             `json:"name,omitempty"`
	FormattedAddress         string             `json:"formatted_address,omitempty"`
	PlaceID                  string             `json:"place_id"`
	Types                    []string           `json:"types,omitempty"`
	Geometry                 Geometry           `json:"geometry,omitempty"`
	Rating                   float64            `json:"rating,omitempty"`
	UserRatingsTotal         int                `json:"user_ratings_total,omitempty"`
	Photos                   []PlacePhoto       `json:"photos,omitempty"` // Original photos from Google
	PhotoURLs                []string           `json:"photo_urls,omitempty"` // Our constructed URLs
	Reviews                  []PlaceReview      `json:"reviews,omitempty"`
	Website                  string             `json:"website,omitempty"`
	InternationalPhoneNumber string             `json:"international_phone_number,omitempty"`
	FormattedPhoneNumber     string             `json:"formatted_phone_number,omitempty"`
	OpeningHours             *PlaceOpeningHours `json:"opening_hours,omitempty"` // Pointer as it can be null
	Vicinity                 string             `json:"vicinity,omitempty"`
	UTCOffset                int                `json:"utc_offset_minutes,omitempty"` // UTC offset in minutes
	AddressComponents        []struct {
		LongName  string   `json:"long_name"`
		ShortName string   `json:"short_name"`
		Types     []string `json:"types"`
	} `json:"address_components,omitempty"`
}

// PlaceDetailResponse wraps the place detail result and status.
type PlaceDetailResponse struct {
	Result           PlaceDetailResult `json:"result"`
	Status           string            `json:"status"` // e.g., "OK", "NOT_FOUND"
	HTMLAttributions []string          `json:"html_attributions,omitempty"`
	ErrorMessage     string            `json:"error_message,omitempty"` // For status codes other than OK
}
