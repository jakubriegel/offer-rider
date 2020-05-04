<template>
  <v-card class="ma-4">
    <v-card-title>Filtry</v-card-title>
    <v-form class="ma-4">
      <v-select label="Marka samochodu" :items="cars" @input="populateModels"/>
      <v-select label="Model samochodu"  :items="models" :disabled="disabled"/>

      <v-row class="">
        <v-text-field type="number" label="Cena od" class="ma-4"></v-text-field>
        <v-text-field type="number" label="Cena do" class="ma-4"></v-text-field>
      </v-row>

      <v-row class="">
        <v-text-field type="number" label="Rok od" class="ma-4"></v-text-field>
        <v-text-field type="number" label="Rok do" class="ma-4"></v-text-field>
      </v-row>

      <v-row class="">
        <v-text-field
          type="number"
          label="Przebieg od"
          class="ma-4"
        ></v-text-field>
        <v-text-field
          type="number"
          label="Przebieg do"
          class="ma-4"
        ></v-text-field>
      </v-row>

      <v-select label="Rodzaj paliwa"></v-select>
    </v-form>
  </v-card>
</template>

<script>
import cars from "./cars.json"
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
      models: ["none"],
      disabled: true
    };
  },
  created() {
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
    }
  }
};
</script>

<style scoped></style>
