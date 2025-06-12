package routes

import (
	"api-service/controllers"

	"github.com/gin-gonic/gin"
)

func RegisterRoutes(r *gin.Engine) {

	maps := r.Group("/maps")
	{
		// Example: GET /maps/geocode?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA
		maps.GET("/geocode", controllers.GeocodeController)
		// Example: GET /maps/places?query=restaurant&location=37.4224764,-122.0842499
		maps.GET("/places", controllers.LocationDataController)
		// Example: GET /maps/directions?origin=Toronto&destination=Montreal
		maps.GET("/directions", controllers.DirectionsController)
		// Example: GET /maps/distance-matrix?origins=Seattle&destinations=San+Francisco
		maps.GET("/distance-matrix", controllers.DistanceMatrixController)
	}
	gemini := r.Group("/gemini")
	{
		// Example: GET /gemini?prompt=explain+about+life+100+words
		gemini.GET("/", controllers.GeminiController)
	}

	test := r.Group("/test")
	{
		test.GET("/", func(c *gin.Context) {
			c.String(200, "hello world")
		})
	}
}
