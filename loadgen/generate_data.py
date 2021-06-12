import json
import requests
import uuid
import random

from common import get_ip_map, UserBase, get_random_time

num_stations = random.randint(20, 100)
num_users = random.randint(100, 1000)

class DataGenerator(UserBase):
    def __init__(self, username, password, ip_map, port_map):
        super().__init__(username, password, ip_map, port_map)

    def getAllTrips(self):
        self.login()
        response = requests.get(self.get_addr('travel-service', "/api/v1/travelservice/trips"), headers=self.auth_headers())
        return json.loads(response._content)['data']

    def getAllStations(self):
        response = requests.get(self.get_addr('station-service', "/api/v1/stationservice/stations"), headers=self.auth_headers())
        return json.loads(response._content)['data']

    def getAllRoutes(self):
        response = requests.get(self.get_addr('route-service', "/api/v1/routeservice/routes"), headers=self.auth_headers())
        return json.loads(response._content)['data']

    def get_random_routes_and_trips(self, stations, num):
        routes = []
        trips = []
        for i in range(num):
            route_ids = random.sample(range(0, len(stations)), random.randint(2, len(stations)))
            station_list = [stations[i]['id'] for i in route_ids]
            distance_list = [random.randint(10, 100) for i in route_ids]
            distance_list.sort()
            route_info = {
                "id": str(uuid.uuid4()),
                "startStation": stations[route_ids[0]]['name'],
                "endStation": stations[route_ids[-1]]['name'],
                "stationList": ','.join(station_list),
                "distanceList": ','.join([str(i) for i in distance_list])
            }
            routes.append(route_info)
            start_time = get_random_time(100)
            end_time = start_time + random.randint(1, 6) * 60 * 60 * 1000
            trips.append({
                "tripId": random.choice(['G', 'D']) + str(random.randint(1000, 9999)),
                "trainTypeId": "GaoTieOne",
                "routeId": route_info['id'],
                "startingStationId": stations[route_ids[0]]['id'],
                "stationsId": stations[route_ids[1]]['id'],
                "terminalStationId": stations[route_ids[-1]]['id'],
                "startingTime": start_time,
                "endTime": end_time
            })
        return routes, trips

    def create_random_stations(self, num):
        return [{"id": "id" + str(i), "name": "name" + str(i), "stayTime": random.randint(0, 20)} for i in range(num)]

    def create_random_users(self, num):
        return [{
            "userId": str(uuid.uuid4()),
            "userName": "uname" + str(i),
            "password":  "pass" + str(i),
            "gender": random.choice([0, 1]),
            "documentType": random.choice([0, 1, 2]),
            "documentNum": random.randint(0, 1000),
            "email": "email" + str(i) + "@email.com"
        } for i in range(num)]

    def set_random_data(self, num_stations, num_users, num_routes, num_foods):
        self.login()

        foods = []
        for i in len(num_foods):
            foods.append({
                "foodName": "food_name" + str(i),
                "price": random.randint(10, 100) * 1.0
            })

        stations = self.create_random_stations(num_stations)
        for station in stations:
            response = requests.post(self.get_addr('station-service', "/api/v1/stationservice/stations"),
                                     json = station, headers=self.auth_headers())
            print(response)

            fid = uuid.uuid4()
            foodstore = {
                "id": fid,
                "stationId": station['id'],
                "storeName": str(fid),
                "telephone": str(random.randint(111111, 999999)),
                "businessTime": str(get_random_time(100)),
                "deliveryFee": random.randint(1, 100),
                "foodList": random.sample(foods, random.randint(1, len(foods) - 1))
            }
            response = requests.post(self.get_addr('food-map-service', "/api/v1/foodmapservice/foodstores/create"),
                                     json = foodstore, headers=self.auth_headers())

        routes, trips = self.get_random_routes_and_trips(stations, num_routes)
        for route in routes:
            response = requests.post(self.get_addr('route-service', "/api/v1/routeservice/routes"),
                                     json = route, headers=self.auth_headers())
            print(response)

        for trip in trips:
            response = requests.post(self.get_addr('travel-service', "/api/v1/travelservice/trips"),
                                     json = trip, headers=self.auth_headers())
            print(response)

            train_food = {
                "id": uuid.uuid4(),
                "tripId": trip['tripId'],
                "foodList": random.sample(foods, random.randint(1, len(foods) - 1))
            }
            response = requests.post(self.get_addr('food-map-service', "/api/v1/foodmapservice/trainfoods/create"),
                                     json = train_food, headers=self.auth_headers())


        # users = self.create_random_users(num_users)
        # for user in users:
            # response = requests.post(self.get_addr('admin-user-service', "/api/v1/adminuserservice/users"),
                                     # json = user, headers=self.auth_headers())
            # print(response)


ip_map, port_map = get_ip_map("3a4205d9a390")
# generator = DataGenerator('fdse_microservice', '111111', ip_map, port_map)
generator = DataGenerator('admin', '222222', ip_map, port_map)
# generator.login()
generator.set_random_data(10, 10, 4, 7)
# generator.test()
# users = create_random_users(10)
# stations = create_random_stations(10)
# print(users)
# print(stations)
# print(get_random_routes(stations, 2))
