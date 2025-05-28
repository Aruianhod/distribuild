import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

interface RealtimeRecommendationChartProps {
  data: any[];
}

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

const RealtimeRecommendationChart: React.FC<RealtimeRecommendationChartProps> = ({ data }) => {
  // 处理数据，统计各类别的推荐数量
  const categoryData: Record<string, number> = {};
  
  data.forEach(item => {
    if (item.category) {
      if (!categoryData[item.category]) {
        categoryData[item.category] = 0;
      }
      categoryData[item.category]++;
    }
  });
  
  // 转换为饼图数据格式
  const pieData = Object.keys(categoryData).map(category => ({
    name: category,
    value: categoryData[category]
  }));

  // 处理时间序列数据
  const timeSeriesData = data.map(item => {
    const timestamp = new Date(item.timestamp * 1000).toLocaleTimeString();
    return {
      time: timestamp,
      category: item.category
    };
  });

  return (
    <div>
      <ResponsiveContainer width="100%" height={150}>
        <PieChart>
          <Pie
            data={pieData}
            cx="50%"
            cy="50%"
            outerRadius={60}
            fill="#8884d8"
            dataKey="value"
            nameKey="name"
            label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
          >
            {pieData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip />
        </PieChart>
      </ResponsiveContainer>
      
      <ResponsiveContainer width="100%" height={150}>
        <LineChart
          data={timeSeriesData.slice(-10)} // 只显示最近10条数据
          margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="time" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Line type="monotone" dataKey="category" name="推荐类别" stroke="#8884d8" />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
};

export default RealtimeRecommendationChart;
