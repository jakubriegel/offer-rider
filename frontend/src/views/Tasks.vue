<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card class="pt-4">
          <v-select
            v-model="searchId"
            label="Searches"
            class="pa-4"
            :items="searches"
            item-text="name"
            item-value="id"
            @input="getTasks"
          />
          <v-select
            v-model="task"
            label="Task"
            class="pa-4"
            :items="tasks"
            item-text="startTime"
            item-value="id"
            @input="showTask"
            :disabled="!searchId"
          />
          <v-card-title>
            Scrapped cars
            <v-spacer></v-spacer>
            <v-text-field
              v-model="search"
              label="Szukaj"
              single-line
              hide-details
              append-icon="mdi-magnify"
            >
            </v-text-field>
          </v-card-title>

          <v-data-table
            :headers="headers"
            :items="results.results"
            :search="search"
            :expanded.sync="expanded"
            single-expand
            :loading="loading"
            item-key="url"
            show-expand
          >
            <template v-slot:item.imgUrl="{ item }">
              <a :href="item.url" target="blank"
                ><v-img :src="item.imgUrl" max-height="125" width="150" contain
              /></a>
            </template>
            <template v-slot:item.url="{ item }">
              <a :href="item.url" target="blank"
                ><v-icon color="black">mdi-open-in-new</v-icon></a
              >
            </template>
            <template v-slot:item.place="{ item }">
              {{ item.params.town }}, {{ item.params.region }}
            </template>
            <template v-slot:expanded-item="{ headers, item }">
              <td :colspan="headers.length">
                More information: {{ item.params }}
              </td>
            </template>
          </v-data-table>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import axios from "axios";
import service from "../config/service.js";
import moment from "moment";

export default {
  name: "table-result",
  data: () => ({
    search: "",
    headers: [
      {
        text: "Image",
        value: "imgUrl",
        sortable: false,
        width: 150
      },
      {
        text: "Title",
        value: "title",
        width: 125
      },
      {
        text: "Description",
        value: "subtitle"
      },
      {
        text: "Price",
        value: "price",
        align: "end",
        width: 100
      },
      {
        text: "Currency",
        value: "currency",
        sortable: false
      },
      {
        text: "Place",
        value: "place"
      },
      {
        text: "Link",
        value: "url",
        sortable: false
      },
      {
        text: "Parameters",
        value: "data-table-expand"
      }
    ],
    results: [],
    expanded: [],
    tasks: [],
    loading: false,
    task: null,
    searches: [],
    searchId: null
  }),
  mounted() {
    axios.get(service.baseUrl + "/search?userId=1").then(response => {
      this.searches = response.data.searches;
      this.searches = this.searches.map(search => ({
        ...search,
        name: `${search.id} - ${search.params.brand} - ${search.params.model}`
      }));
    });
  },
  methods: {
    getTasks() {
      axios
        .get(service.baseUrl + "/tasks?userId=1&searchId=" + this.searchId)
        .then(
          response => (
            (this.tasks = response.data.tasks),
            this.tasks.sort(function(a, b) {
              if (a.startTime < b.startTime) {
                return 1;
              } else if (a.startTime > b.startTime) {
                return -1;
              } else return 0;
            }),
            this.formatDate(this.tasks)
          )
        );
    },
    showTask() {
      this.loading = true;
      axios
        .get(
          service.baseUrl + "/results?userId=1&searchId=1&taskId=" + this.task
        )
        .then(response => {
          this.results = response.data;
        })
        .then(() => {
          this.loading = false;
        });
    },
    formatDate(tasks) {
      for (const id in tasks) {
        tasks[id].startTime = moment
          .utc(tasks[id].startTime)
          .format("YYYY MMM D dddd, HH:mm");
      }
    }
  }
};
</script>

<style scoped>
a {
  text-decoration: none;
}
</style>
