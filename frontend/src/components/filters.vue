<template>
  <v-card class="ma-4">
    <v-card-title>Filtry</v-card-title>
    <v-form class="ma-4">
      <v-select v-model="params.brand" label="Marka samochodu" :items="cars" @input="populateModels" clearable/>
      <v-select v-model="params.model" label="Model samochodu"  :items="models" :disabled="disabled" clearable/>

      <v-row class="">
        <v-text-field v-model="params.minPrice" type="number" label="Cena od" class="ma-4"></v-text-field>
        <v-text-field v-model="params.maxPrice" type="number" label="Cena do" class="ma-4"></v-text-field>
      </v-row>

      <v-row class="">
        <v-text-field v-model="params.minYear" type="number" label="Rok od" class="ma-4"></v-text-field>
        <v-text-field v-model="params.maxYear" type="number" label="Rok do" class="ma-4"></v-text-field>
      </v-row>

      <v-row class="">
        <v-text-field
                v-model="params.minMileage"
                type="number"
                label="Przebieg od"
                class="ma-4"
        ></v-text-field>
        <v-text-field
                v-model="params.maxMileage"
                type="number"
                label="Przebieg do"
                class="ma-4"
        ></v-text-field>
      </v-row>

      <v-select v-model="params.fuel" label="Rodzaj paliwa" :items="fuelType" clearable></v-select>
      <v-btn @click="sendFilters">
        Utwórz wyszukiwanie
      </v-btn>
    </v-form>
  </v-card>
</template>

<script>
import cars from "./cars.json"
import axios from "axios";
const service = "http://jrie.eu:30001";
export default {
  name: "filters",
  data() {
    return {
      min: 1990,
      max: 2020,
      slider: 0,
      range: [1990, 2020],
      result: [],
      cars: [],
      fuelType: [
              "petrol",
              "diesel",
              "all"
      ],
      models: ["none"],
      disabled: true,
      params: {
        brand: null,
        model: null,
        fuel: null,
        maxPrice: null,
        minPrice: null,
        maxEngineSize: null,
        minEngineSize: null,
        maxYear: null,
        minYear: null,
        minMileage: null,
        maxMileage: null
      }
    };
  },
  mounted() {
    var temp = []
            Object.keys(cars.cars).forEach(car => {
      temp.push(car.charAt(0).toUpperCase() + car.slice(1))
    })
    this.cars = temp
  },
  methods: {
    populateModels(test) {
      test = test.toString().toLowerCase()
      this.disabled = false;
      console.log(test)
      this.models = cars.cars[test].map(function (obj) {
        return obj.value
      })
    },
    sendFilters() {
      axios.post(service + '/search', {
        userId: 1,
        params: this.params
      })
    }
  }
};
</script>

<style scoped></style>
