<script setup>
defineProps({ title:String, subtitle:String, label:{type:String,default:'TRIPASS PICKS'} })
</script>
<template><section class="ticket"><small>{{ label }}</small><h2>{{ title }}</h2><p>{{ subtitle }}</p><span>|||||||</span></section></template>
<style scoped>
.ticket{position:relative;padding:16px 15px;border-radius:15px;background:linear-gradient(135deg,#17387f,#2465c3);color:#fff;box-shadow:0 8px 18px #17387f2b}.ticket:before,.ticket:after{position:absolute;top:50%;width:12px;height:12px;border-radius:50%;background:#f7f5ef;content:''}.ticket:before{left:-6px}.ticket:after{right:-6px}.ticket small{color:#a9c9ff;font-size:7px;letter-spacing:.08em}.ticket h2{margin-top:10px;font-size:15px;font-weight:900}.ticket p{margin-top:4px;color:#cfdeff;font-size:8px}.ticket>span{position:absolute;right:13px;bottom:8px;color:#a9c9ff;font-size:7px;letter-spacing:1px}
</style>
