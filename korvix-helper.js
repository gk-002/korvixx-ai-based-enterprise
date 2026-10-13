/**
 * KORVIX High-Performance File Pipeline
 * Server-Side Stream Aggregator (Node.js Helper)
 * 
 * Ingests a CSV/Excel file stream (up to 10,000+ records) and performs
 * lightweight data aggregation to summarize row counts by Department,
 * Date, and Performance rating. This prevents web client crashes and
 * ensures sub-second aggregation times.
 */

const fs = require('fs');
const readline = require('readline');

/**
 * Ingests a CSV file stream and returns an aggregated dashboard-friendly payload.
 * Expected columns: department, date, performance (rating 1-5 or descriptive label)
 * 
 * @param {string} filePath - Path to the uploaded CSV file
 * @returns {Promise<object>} Aggregated metrics object
 */
async function aggregateFileStream(filePath) {
  return new Promise((resolve, reject) => {
    const fileStream = fs.createReadStream(filePath);
    
    const rl = readline.createInterface({
      input: fileStream,
      crlfDelay: Infinity
    });

    let isHeader = true;
    let headerMap = {};
    let totalRows = 0;

    // Aggregators
    const deptDistribution = {};
    const performanceCounts = {
      "Excellent (5)": 0,
      "Very Good (4)": 0,
      "Good (3)": 0,
      "Needs Improvement (2)": 0,
      "Unsatisfactory (1)": 0
    };
    const dateTrend = {};

    rl.on('line', (line) => {
      // Split by comma while respecting quotes if any
      const columns = line.split(/,(?=(?:(?:[^"]*"){2})*[^"]*$)/);
      
      if (isHeader) {
        // Map header indices
        columns.forEach((col, idx) => {
          const cleanCol = col.replace(/"/g, '').trim().toLowerCase();
          headerMap[cleanCol] = idx;
        });
        isHeader = false;
        return;
      }

      totalRows++;

      // Extract values based on headers (fallback to default indices if not found)
      const deptIdx = headerMap['department'] !== undefined ? headerMap['department'] : 0;
      const dateIdx = headerMap['date'] !== undefined ? headerMap['date'] : 1;
      const perfIdx = headerMap['performance'] !== undefined ? headerMap['performance'] : 2;

      const rawDept = columns[deptIdx]?.replace(/"/g, '').trim() || 'Other';
      const rawDate = columns[dateIdx]?.replace(/"/g, '').trim() || 'Unknown';
      const rawPerf = columns[perfIdx]?.replace(/"/g, '').trim() || '3';

      // 1. Department aggregation
      deptDistribution[rawDept] = (deptDistribution[rawDept] || 0) + 1;

      // 2. Performance aggregation (Map numeric to descriptive or bucket directly)
      const perfNum = parseFloat(rawPerf);
      if (perfNum >= 4.5 || rawPerf === 'Excellent' || rawPerf === '5') {
        performanceCounts["Excellent (5)"]++;
      } else if (perfNum >= 3.5 || rawPerf === 'Very Good' || rawPerf === '4') {
        performanceCounts["Very Good (4)"]++;
      } else if (perfNum >= 2.5 || rawPerf === 'Good' || rawPerf === '3') {
        performanceCounts["Good (3)"]++;
      } else if (perfNum >= 1.5 || rawPerf === 'Needs Improvement' || rawPerf === '2') {
        performanceCounts["Needs Improvement (2)"]++;
      } else {
        performanceCounts["Unsatisfactory (1)"]++;
      }

      // 3. Date / Timeline trend (bucket to keep trend readable)
      dateTrend[rawDate] = (dateTrend[rawDate] || 0) + 1;
    });

    rl.on('close', () => {
      // Structure the aggregated response
      const results = {
        success: true,
        summary: {
          totalRowsParsed: totalRows,
          timestamp: new Date().toISOString()
        },
        deptDistribution: Object.entries(deptDistribution).map(([name, count]) => ({
          name,
          count,
          percentage: totalRows > 0 ? ((count / totalRows) * 100).toFixed(1) + '%' : '0%'
        })),
        performanceMetrics: Object.entries(performanceCounts).map(([rating, count]) => ({
          rating,
          count,
          percentage: totalRows > 0 ? ((count / totalRows) * 100).toFixed(1) + '%' : '0%'
        })),
        dateTrend: Object.entries(dateTrend)
          .map(([date, count]) => ({ date, count }))
          .sort((a, b) => new Date(a.date) - new Date(b.date))
          .slice(-15) // Limit to latest 15 dates for crisp chart display
      };

      resolve(results);
    });

    rl.on('error', (err) => {
      reject({
        success: false,
        error: err.message
      });
    });
  });
}

module.exports = { aggregateFileStream };
