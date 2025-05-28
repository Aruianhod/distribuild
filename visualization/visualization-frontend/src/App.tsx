import { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import { Layout, Menu, theme } from 'antd';
import {
  DashboardOutlined,
  UserOutlined,
  GiftOutlined,
  LineChartOutlined,
} from '@ant-design/icons';

// 页面组件
import Dashboard from './components/Dashboard';
import UserBehavior from './components/UserBehavior';
import Recommendation from './components/Recommendation';
import SystemPerformance from './components/SystemPerformance';

import './App.css';

const { Header, Content, Footer, Sider } = Layout;

function App() {
  const [collapsed, setCollapsed] = useState(false);
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  return (
    <Router>
      <Layout style={{ minHeight: '100vh' }}>
        <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
          <div className="demo-logo-vertical" />
          <Menu theme="dark" defaultSelectedKeys={['1']} mode="inline">
            <Menu.Item key="1" icon={<DashboardOutlined />}>
              <Link to="/">仪表盘</Link>
            </Menu.Item>
            <Menu.Item key="2" icon={<UserOutlined />}>
              <Link to="/user-behavior">用户行为分析</Link>
            </Menu.Item>
            <Menu.Item key="3" icon={<GiftOutlined />}>
              <Link to="/recommendation">推荐效果分析</Link>
            </Menu.Item>
            <Menu.Item key="4" icon={<LineChartOutlined />}>
              <Link to="/system-performance">系统性能监控</Link>
            </Menu.Item>
          </Menu>
        </Sider>
        <Layout>
          <Header style={{ padding: 0, background: colorBgContainer }} />
          <Content style={{ margin: '0 16px' }}>
            <div
              style={{
                padding: 24,
                minHeight: 360,
                background: colorBgContainer,
                borderRadius: borderRadiusLG,
              }}
            >
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/user-behavior" element={<UserBehavior />} />
                <Route path="/recommendation" element={<Recommendation />} />
                <Route path="/system-performance" element={<SystemPerformance />} />
              </Routes>
            </div>
          </Content>
          <Footer style={{ textAlign: 'center' }}>
            分布式实时电商推荐系统可视化平台 ©{new Date().getFullYear()}
          </Footer>
        </Layout>
      </Layout>
    </Router>
  );
}

export default App;
