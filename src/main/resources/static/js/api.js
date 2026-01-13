/**
 * API封装
 */
const api = {
  baseURL: '/api',

  async request(method, url, data) {
    try {
      const config = {
        method,
        url: this.baseURL + url,
        headers: { 'Content-Type': 'application/json' }
      };
      if (data) {
        config.data = data;
      }
      const response = await axios(config);
      return response.data;
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  },

  get(url) { return this.request('GET', url); },
  post(url, data) { return this.request('POST', url, data); },
  put(url, data) { return this.request('PUT', url, data); },
  delete(url) { return this.request('DELETE', url); },

  // 仓库API
  warehouse: {
    list(page = 1, pageSize = 10, keyword = '') {
      return api.get(`/v1/warehouses?page=${page}&pageSize=${pageSize}&keyword=${keyword}`);
    },
    get(id) { return api.get(`/v1/warehouses/${id}`); },
    create(data) { return api.post('/v1/warehouses', data); },
    delete(id) { return api.delete(`/v1/warehouses/${id}`); },
    sync(id, force = false) { return api.post(`/v1/warehouses/${id}/sync?forceSync=${force}`); }
  },

  // 领域API
  domain: {
    list(warehouseId) { return api.get(`/v1/warehouses/${warehouseId}/domains`); },
    get(warehouseId, domainId) { return api.get(`/v1/warehouses/${warehouseId}/domains/${domainId}`); },
    create(warehouseId, data) { return api.post(`/v1/warehouses/${warehouseId}/domains`, data); },
    update(warehouseId, domainId, data) { return api.put(`/v1/warehouses/${warehouseId}/domains/${domainId}`, data); },
    delete(warehouseId, domainId) { return api.delete(`/v1/warehouses/${warehouseId}/domains/${domainId}`); },
    generateDoc(warehouseId, domainId) { return api.post(`/v1/warehouses/${warehouseId}/domains/${domainId}/generate-doc`); }
  },

  // 服务API
  service: {
    list(warehouseId, domainId) { return api.get(`/v1/warehouses/${warehouseId}/domains/${domainId}/services`); },
    create(warehouseId, domainId, data) { return api.post(`/v1/warehouses/${warehouseId}/domains/${domainId}/services`, data); },
    update(warehouseId, domainId, serviceId, data) { return api.put(`/v1/warehouses/${warehouseId}/domains/${domainId}/services/${serviceId}`, data); },
    delete(warehouseId, domainId, serviceId) { return api.delete(`/v1/warehouses/${warehouseId}/domains/${domainId}/services/${serviceId}`); },
    generateDoc(warehouseId, domainId, serviceId) { return api.post(`/v1/warehouses/${warehouseId}/domains/${domainId}/services/${serviceId}/generate-doc`); }
  },

  // 文档API
  document: {
    get(warehouseId, path) { return api.get(`/v1/warehouses/${warehouseId}/documents/${path}`); },
    getCatalog(warehouseId) { return api.get(`/v1/warehouses/${warehouseId}/catalog`); }
  },

  // AI文档API
  aiDocument: {
    list(warehouseId) { return api.get(`/v1/warehouses/${warehouseId}/ai-documents`); },
    get(id) { return api.get(`/v1/ai-documents/${id}`); }
  },

  // Agent API
  agent: {
    list() { return api.get('/v1/agents'); },
    get(id) { return api.get(`/v1/agents/${id}`); }
  },

  // 全局领域API
  globalDomain: {
    list() { return api.get('/v1/domains'); },
    get(domainId) { return api.get(`/v1/domains/${domainId}`); },
    create(data) { return api.post('/v1/domains', data); },
    update(domainId, data) { return api.put(`/v1/domains/${domainId}`, data); },
    delete(domainId) { return api.delete(`/v1/domains/${domainId}`); },
    generateDoc(domainId) { return api.post(`/v1/domains/${domainId}/generate-doc`); },
    listServices(domainId) { return api.get(`/v1/domains/${domainId}/services`); },
    createService(domainId, data) { return api.post(`/v1/domains/${domainId}/services`, data); },
    generateServiceDoc(domainId, serviceId) { return api.post(`/v1/domains/${domainId}/services/${serviceId}/generate-doc`); },
    listWarehouses() { return api.get('/v1/domains/warehouses'); }
  }
};
