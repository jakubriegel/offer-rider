<template>
  <v-footer dark color="primary">
    <v-card flat tile class="grey darken-4 white--text text-center">
      <v-card-text>
        <v-btn
          v-for="link in socialMedia"
          :key="link.icon"
          class="mx-4 white--text"
          icon
          :href="link.link"
        >
          <v-icon size="24px" color="accent">{{ link.icon }}</v-icon>
        </v-btn>
      </v-card-text>

      <v-row>
        <v-col cols="12" md="9" class="px-5">
          <v-card-text class="white--text pt-0 text-left">
            Software Engineering Project Description
          </v-card-text>
          <v-card-text class="white--text pt-0 text-left">
            Made with 💖 using
            <strong class="accent--text">Scala, Cheerio, Vue</strong> and
            <strong class="accent--text">Vuetify. </strong>
            Scrapped magnificient from 
            <a href="https://www.otomoto.pl/" class="accent--text">OtoMoto</a>
          </v-card-text>
        </v-col>
        <v-col cols="12" md="3" class="max-height pt-0">
          <v-img
            :src="ppLogo"
            alt="Politechnika Poznańska Logo"
            height="80"
            contain
          />
        </v-col>
      </v-row>

      <v-divider />

      <v-card-text class="white--text">
        {{ new Date().getFullYear() }} —
        <strong>
          &copy;
          <a
            href="mailto:wojciech.kasperski97@gmail.com"
            class="font-weight-light white--text"
          >
            Wojciech Kasperski
          </a>
          , Jakub Riegel, Maciej Stosik
        </strong>
      </v-card-text>
    </v-card>
  </v-footer>
</template>

<script>
export default {
  name: "Footer",
  data: () => ({
    socialMedia: [
      {
        icon: "mdi-github",
        link: "https://github.com/jakubriegel/scrapper-search-service"
      },
      {
        icon: "mdi-vuejs",
        link: "https://vuejs.org/"
      },
      {
        icon: "mdi-vuetify",
        link: "https://vuetifyjs.com/en/"
      },
      {
        icon: "mdi-language-kotlin",
        link: "https://spring.io/"
      }
    ],
    ppLogo: require("../assets/PolitechnikaPoznanska.png")
  })
};
</script>
