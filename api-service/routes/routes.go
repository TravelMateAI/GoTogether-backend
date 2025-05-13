package routes

import (
	"api-service/controllers"

	"github.com/gin-gonic/gin"
)

func RegisterRoutes(r *gin.Engine) {
	api := r.Group("/api")
	{
		maps := api.Group("/maps")
		{
			maps.GET("/geocode", controllers.GeocodeController)
			maps.GET("/places", controllers.LocationDataController)
		}
		gemini := api.Group("/gemini")
		{
			gemini.GET("/location-summery", controllers.LocationSummeryController)
		}
	}
	test := r.Group("/test")
	{
		test.GET("/", func(c *gin.Context) {
			c.String(200, "hello world")
		})
	}
}
