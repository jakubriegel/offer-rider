<template>
  <v-container class="pt-0 pb-4" v-if="!!parameters">
    <v-row class="px-2">
      <v-col cols="12">
        <h3>Search Parameters</h3>
      </v-col>
      <v-col cols="12" sm="6" class="py-0">
        <strong>Search ID: </strong>{{ parameters.id }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0">
        <strong>User ID: </strong>{{ parameters.userId }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.brand">
        <strong>Brand: </strong>{{ parameters.params.brand }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.model">
        <strong>Model: </strong>{{ parameters.params.model }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.price_from">
        <strong>Price from: </strong>{{ parameters.params.price_from }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.price_to">
        <strong>Price to: </strong>{{ parameters.params.price_to }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.year_from">
        <strong>Year from: </strong>{{ parameters.params.year_from }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.year_to">
        <strong>Year to: </strong>{{ parameters.params.year_to }}
      </v-col>
      <v-col
        cols="12"
        sm="6"
        class="py-0"
        v-if="parameters.params.mileage_from"
      >
        <strong>Mileage from: </strong>{{ parameters.params.mileage_from }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0" v-if="parameters.params.mileage_to">
        <strong>Mileage to: </strong>{{ parameters.params.mileage_to }}
      </v-col>
      <v-col cols="12" sm="6" class="py-0">
        <strong>Current status: </strong>
        <span class="green--text" v-if="parameters.active">ACTIVE</span>
        <span class="red--text" v-else>DEACTIVATED</span>
      </v-col>
    </v-row>
    <v-row class="pt-2 pl-4">
      <v-col cols="12" class="pa-0">
        <v-btn v-if="loading" disabled
          ><v-icon>mdi-spin mdi-loading</v-icon></v-btn
        >
        <v-btn
          v-else-if="parameters.active"
          color="red"
          class="white--text"
          @click="setDeactivated"
        >
          DEACTIVATE
        </v-btn>
        <v-btn v-else color="green" class="white--text" @click="setActive">
          ACTIVATE
        </v-btn>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import axios from "axios";
import service from "../config/service.js";

export default {
  name: "SearchParameters",
  props: {
    parameters: Object
  },
  data: () => ({
    loading: false
  }),
  methods: {
    setActive() {
      this.loading = true;
      axios
        .put(service.baseUrl + "/search/" + this.parameters.id + "/activate")
        .then(() => {
          this.parameters.active = true;
        })
        .then(() => {
          this.loading = false;
        });
    },
    setDeactivated() {
      axios
        .put(service.baseUrl + "/search/" + this.parameters.id + "/deactivate")
        .then(() => {
          this.parameters.active = false;
        })
        .then(() => {
          this.loading = false;
        });
    }
  }
};
</script>
