package search.expose;

public class Article {
    private String title;
    private String url;
    private String txt;
    private String date;
    private String lang;

    public Article(String title, String url, String txt, String date, String lang) {
        this.title = title;
        this.url = url;
        this.txt = txt;
        this.date = date;
        this.lang = lang;
    }

    public Article(String title, String url, String txt) {
        this.title = title;
        this.url = url;
        this.txt = txt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTxt() {
        return txt;
    }

    public void setTxt(String txt) {
        this.txt = txt;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}