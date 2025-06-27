package routes

import (
	"api-service/controllers"

	"github.com/gin-gonic/gin"
)

func RegisterRoutes(r *gin.Engine) {

	maps := r.Group("/search")
	{

	}

	test := r.Group("/test")
	{
		test.GET("/", func(c *gin.Context) {
			c.String(200, "hello world")
		})
	}

	r.GET("/healthz", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"status": "ok",
		})
	})
}
