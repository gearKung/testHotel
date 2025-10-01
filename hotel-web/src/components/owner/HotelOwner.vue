<template>
  <div class="owner-page">
    <aside class="sidebar">
      <div class="logo">🏨 Hotel</div>
      <nav>
        <ul>
          <li :class="{ active: activeMenu === 'OwnerDashboard' }" @click="navigateTo('OwnerDashboard')">대시보드</li>
          <li :class="{ active: activeMenu === 'OwnerHotel' }" @click="navigateTo('OwnerHotel')">호텔/객실 관리</li>
          <li :class="{ active: activeMenu === 'OwnerReservation' }" @click="navigateTo('OwnerReservation')">예약 관리</li>
          <li :class="{ active: activeMenu === 'OwnerReview' }" @click="navigateTo('OwnerReview')">리뷰 관리</li>
        </ul>
      </nav>

      <div class="sidebar-footer">
        <button class="btn-homepage" @click="$router.push('/')">홈페이지</button>
        <button class="btn-logout-sidebar" @click="logoutAndGoHome">로그아웃</button>
      </div>
    </aside>

    <main class="main-content">
      <router-view :user="user" @logout="logoutAndGoHome"></router-view>
    </main>
  </div>
</template>

<script>
export default {
  data() {
    return {
      user: null,
    };
  },
  computed: {
    activeMenu() {
      return this.$route.name;
    }
  },
  methods: {
    checkLoginStatus() {
      const userInfo = localStorage.getItem('user');
      if (userInfo) {
        this.user = JSON.parse(userInfo);
      } else {
        this.$router.push("/login");
      }
    },
    navigateTo(routeName) {
      if (this.$route.name !== routeName) {
        this.$router.push({ name: routeName });
      }
    },
    logoutAndGoHome() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        alert("로그아웃 되었습니다.");
        this.$router.push('/');
    },
  },
  mounted() {
    this.checkLoginStatus();
  }
}
</script>

<style scoped>
/* 전체 레이아웃 */
.owner-page { display: flex; height: 100vh; width: 100vw; margin: 0; background: #f3f4f6; }
.sidebar { width: 220px; background: #111827; color: #fff; padding: 20px 10px; box-sizing: border-box; position: fixed; top: 0; left: 0; bottom: 0; overflow-y: auto; z-index: 10; display: flex; flex-direction: column; }
.sidebar nav { flex-grow: 1; }
.sidebar .logo { font-weight: 700; font-size: 20px; margin-bottom: 25px; text-align: center; }
.sidebar ul { list-style: none; padding: 0; margin: 0; }
.sidebar li { padding: 12px 15px; cursor: pointer; border-radius: 6px; margin: 4px 0; transition: background-color .2s; }
.sidebar li.active, .sidebar li:hover { background: #374151; }
.sidebar-footer { padding: 10px; display: flex; flex-direction: column; gap: 10px; }
.sidebar-footer button { width: 100%; padding: 12px; border: none; border-radius: 6px; color: white; font-size: 15px; font-weight: 700; cursor: pointer; transition: background-color 0.2s; }
.btn-homepage { background-color: #4B5563; }
.btn-homepage:hover { background-color: #374151; }
.btn-logout-sidebar { background-color: #a92a2a; }
.btn-logout-sidebar:hover { background-color: #8a2020; }
.main-content { margin-left: 220px; width: calc(100% - 220px); height: 100vh; padding: 0; box-sizing: border-box; overflow-y: auto; }
</style>