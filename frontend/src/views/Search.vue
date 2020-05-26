<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card class="pa-2">
          <v-card-title>Filters</v-card-title>
          <v-form class="ma-4">
            <v-select
              v-model="params.brand"
              label="Car brand"
              :items="cars"
              @input="populateModels"
              clearable
            />
            <v-select
              v-model="params.model"
              label="Car model"
              :items="models"
              :disabled="disabled"
              clearable
            />

            <v-row>
              <v-text-field
                v-model="params.price_from"
                type="number"
                label="Price from"
                class="ma-4"
              ></v-text-field>
              <v-text-field
                v-model="params.price_to"
                type="number"
                label="Price to"
                class="ma-4"
              ></v-text-field>
            </v-row>

            <v-row>
              <v-text-field
                v-model="params.year_from"
                type="number"
                label="Year from"
                class="ma-4"
              ></v-text-field>
              <v-text-field
                v-model="params.year_to"
                type="number"
                label="Year to"
                class="ma-4"
              ></v-text-field>
            </v-row>

            <v-row>
              <v-text-field
                v-model="params.mileage_from"
                type="number"
                label="Mileage from"
                class="ma-4"
              ></v-text-field>
              <v-text-field
                v-model="params.mileage_to"
                type="number"
                label="Mileage to"
                class="ma-4"
              ></v-text-field>
            </v-row>

            <!-- <v-select v-model="params.fuel" label="Rodzaj paliwa" :items="fuelType" clearable></v-select> -->
            <v-btn
              @click="sendFilters"
              color="accent"
              class="mb-4 black--text"
              block
              :disabled="loading"
            >
              <span v-if="!loading">Create Search Task</span>
              <v-icon v-else>mdi-spin mdi-loading</v-icon>
            </v-btn>
          </v-form>
        </v-card>
      </v-col>
    </v-row>
    <v-snackbar
      v-model="showInfo"
      bottom
      right
      :color="error ? 'red' : 'green'"
      multi-line
      :timeout="10000"
    >
      {{ error ? error : "Successfully added task!" }}
      <v-btn dark text @click="showInfo = false" fab>
        <v-icon>mdi-close</v-icon>
      </v-btn>
    </v-snackbar>
  </v-container>
</template>

<script>
import cars from "../mocks/cars.json";
import axios from "axios";
import service from "../config/service.js";
export default {
  name: "filters",
  data: () => ({
    min: 1990,
    max: 2020,
    slider: 0,
    range: [1990, 2020],
    result: [],
    cars: [],
    fuelType: ["petrol", "diesel", "all"],
    models: ["none"],
    disabled: true,
    loading: false,
    showInfo: false,
    error: null,
    params: {
      brand: null,
      model: null,
      price_to: null,
      price_from: null,
      year_to: null,
      year_from: null,
      mileage_from: null,
      mileage_to: null
    }
  }),
  mounted() {
    const temp = [];
    Object.keys(cars.cars).forEach(car => {
      temp.push(car.charAt(0).toUpperCase() + car.slice(1));
    });
    this.cars = temp;
  },
  methods: {
    populateModels(test) {
      test = test.toString().toLowerCase();
      this.disabled = false;
      this.models = cars.cars[test].map(function(obj) {
        return obj.value;
      });
    },
    sendFilters() {
      this.loading = true;
      this.error = null;
      axios
        .post(service.baseUrl + "/search", {
          userId: 1,
          params: this.params
        })
        .catch(e => {
          this.error = e;
        })
        .then(() => {
          this.loading = false;
          this.showInfo = true;
        });
    }
  }
};
</script>

<style scoped></style>
