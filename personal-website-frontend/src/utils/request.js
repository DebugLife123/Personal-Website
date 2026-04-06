import axios from 'axios'

const request = axios.create({
    baseURL: 'http://localhost:8080/api', // 后端接口基础路径
    timeout: 5000
})

export default request