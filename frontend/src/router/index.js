import Vue from "vue";
import VueRouter from "vue-router";

Vue.use(VueRouter);

const routes = [{
        path: "/",
        name: "Dashboard",
        component: () =>
            import("../views/Search.vue")
    },
    {
        path: "/tasks",
        name: "Tasks",
        component: () =>
            import("../views/Tasks.vue")
    }
];

const router = new VueRouter({
    routes,
    mode: "history",
    base: process.env.NODE_ENV === "production" ?
        "/" : "/"
});

export default router;