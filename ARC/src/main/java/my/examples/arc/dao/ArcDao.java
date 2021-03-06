package my.examples.arc.dao;

import my.examples.arc.dto.ARCGdsMstDto;
import my.examples.arc.dto.ARCInvInputDto;
import my.examples.arc.dto.ARCReplyDto;
import my.examples.arc.dto.MyGoodsListDto;

import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class ArcDao {
    private String dbURL=null;
    private Properties properties;

    public ArcDao() {
        GetProperties gp = GetProperties.getInstance();
        dbURL = gp.getDbURL();
        properties = gp.getProperties();
    }

    public int addMyGoodsList(ARCInvInputDto arcInvInputDtoParam) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        int count = 0;
        ARCInvInputDto arcInvInputDto = arcInvInputDtoParam;
        conn = DbUtil.connect(dbURL, properties);
        try {
            String sql = null;

            sql = "INSERT INTO my_inv_lst ( id, gds_cd, inv_prod, my_inv_prc)"
                    + "VALUES(?, ?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, "kimId");
            ps.setInt(2, arcInvInputDto.getInvPrdIdx());
            ps.setInt(3, arcInvInputDto.getInvPeriod());
            ps.setInt(4, arcInvInputDto.getInvMoney());
            count = ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            DbUtil.close(conn, ps);
        }

        return count;
    }

    public List<ARCGdsMstDto> getAllGoodsListDto() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        List<ARCGdsMstDto> list = new ArrayList<>();
        conn = DbUtil.connect(dbURL, properties);
        try {
            String sql = null;
            sql = "SELECT * FROM gds_mst ORDER BY gds_cd ASC";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                ARCGdsMstDto arcGdsMstDto = new ARCGdsMstDto();
                arcGdsMstDto.setGds_cd(rs.getInt(1));
                arcGdsMstDto.setGds_nm(rs.getString(2));
                list.add(arcGdsMstDto);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            DbUtil.close(conn, ps, rs);
        }
        return list;
    }

    public List<MyGoodsListDto> getMyGoodsListDto(String pg, int posts) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        conn = DbUtil.connect(dbURL,properties);

        List<MyGoodsListDto> list = new ArrayList<>();
        try{
            String sql=null;
            // 투자리스트 게시판
            sql ="SET @rownum:=0;";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            sql =   "SELECT A.*\n" +
                    "FROM (SELECT @rownum:=@rownum+1 AS ROW_NUM,\n" +
                    "m.gds_nm , gds.prf_rto, inv.inv_prod, inv.my_inv_prc, gds.cms\n" +
                    "FROM my_inv_lst inv, inv_gds_lst gds INNER JOIN gds_mst m ON gds.gds_cd = m.gds_cd\n" +
                    "WHERE inv.gds_cd = gds.gds_cd)A \n" +
                    "WHERE A.ROW_NUM BETWEEN ? AND ?;";
            ps = conn.prepareStatement(sql);
            ps.setInt(1,Integer.parseInt(pg)*posts-(posts-1));
            ps.setInt(2,Integer.parseInt(pg)*posts);
            rs = ps.executeQuery();

            while(rs.next()) {
                MyGoodsListDto myGoodsListDto = new MyGoodsListDto();
                myGoodsListDto.setRownum(rs.getInt(1));
                myGoodsListDto.setGoodsName(rs.getString(2));
                myGoodsListDto.setPrfRto(rs.getLong(3));
                myGoodsListDto.setInvestPeriod(rs.getInt(4));
                myGoodsListDto.setMyPrice(rs.getInt("A.my_inv_prc"));
                myGoodsListDto.setCms(rs.getDouble("cms"));
                myGoodsListDto.setProfits(myGoodsListDto.getMyPrice()+myGoodsListDto.getMyPrice()*myGoodsListDto.getPrfRto()/100);
                list.add(myGoodsListDto);
            }

        }catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            DbUtil.close(conn, ps, rs);
        }
        return list;
    }

    public int getCnt(){
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        conn = DbUtil.connect(dbURL,properties);

        int cnt=0;
        try {
            // 총 개수 Query
            String sql ="SELECT COUNT(*)\n" +
                    "FROM my_inv_lst inv, inv_gds_lst gds \n" +
                    "WHERE inv.gds_cd = gds.gds_cd;";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while(rs.next()) {

                cnt=rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cnt;
    }


    // 댓글 등록 SQL 전송
    public int addReply(ARCReplyDto arcReplyDtoParam) {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = DbUtil.connect(dbURL,properties);

        int count = 0;
        ARCReplyDto arcReplyDto = arcReplyDtoParam;

        try {
            String sql = "INSERT INTO mb_rpy (mb_idx, prt_idx, content)"
                + "VALUES(?, ?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, arcReplyDto.getMember_idx());
            ps.setInt(2, arcReplyDto.getParent_idx());
            ps.setString(3, arcReplyDto.getContent());
            count = ps.executeUpdate();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            DbUtil.close(conn, ps);
        }
        return count;
    }

    // 댓글 가져오기
    public List<ARCReplyDto> getReply() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+9"));

        conn = DbUtil.connect(dbURL,properties);

        List<ARCReplyDto> list = new ArrayList<>();

        try {
            String sql = "SELECT member.mb_idx, id, rpy_idx, prt_idx, content, rpy_time\n"
                    + "FROM member INNER JOIN mb_rpy ON member.mb_idx=mb_rpy.mb_idx";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                ARCReplyDto arcReplyDto = new ARCReplyDto();
                arcReplyDto.setMember_idx(rs.getInt(1));
                arcReplyDto.setMember_id(rs.getString(2));
                arcReplyDto.setReply_idx(rs.getInt(3));
                arcReplyDto.setParent_idx(rs.getInt(4));
                arcReplyDto.setContent(rs.getString(5));
                arcReplyDto.setReply_time(rs.getTimestamp(6, cal).getTime());

                list.add(arcReplyDto);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            DbUtil.close(conn, ps, rs);
        }

        return list;
    }

    public int deleteReply(int reply_idx) {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = DbUtil.connect(dbURL,properties);
        int count = 0;

        try {
            String sql = "DELETE FROM mb_rpy WHERE rpy_idx = ?";
            ps = conn.prepareStatement(sql);
            ps.setLong(1, reply_idx);
            count = ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            DbUtil.close(conn, ps);
        }

        return count;
    }

    public int modifyReply(int reply_idx, String content) {
        Connection conn = null;
        PreparedStatement ps = null;
        conn = DbUtil.connect(dbURL,properties);
        int count = 0;

        try {
            String sql = "UPDATE mb_rpy SET content = ? WHERE rpy_idx = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, content);
            ps.setInt(2, reply_idx);
            count = ps.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            DbUtil.close(conn, ps);
        }

        return count;
    }
}