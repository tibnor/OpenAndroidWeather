from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from weatherstation import WeatherStation




class MainPage(webapp.RequestHandler):
    
    def get(self):
        stationId = self.request.get("st")
        station = WeatherStation.get_or_insert(stationId, id=int(stationId))
        self.response.headers['Content-Type'] = 'text/plain'
        responseText = station.getTempNow()
        if responseText == "":
            self.response.set_status(204)
        else:
            self.response.set_status(200)
        self.response.out.write(responseText)


application = webapp.WSGIApplication([('/temperature', MainPage)], debug=True)


def main():
    run_wsgi_app(application)

if __name__ == "__main__":
    main()
    


        


    
        
        
