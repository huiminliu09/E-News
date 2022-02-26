import './Home.css'
import Search from '../components/Search'
import { Pagination, DatePicker, Checkbox, Card, Empty } from 'antd'
import { useSetState } from 'ahooks'
import React, { useRef } from 'react';
import Highlighter from "react-highlight-words";
import logo from "../images/LOGO.png"

const App = ({ serverUrl }) => {
    const [state, setState] = useSetState({
        init: false,
        results: [],
        total: 0,
        page: 1,
        query: '',
    })

    const [params, setParams] = useSetState({
        language: undefined,
        date: undefined,
        offset: 0
    })

    const wrap = useRef()

    // Highlight
    const getHighLightStr = function (value, key) {
        value = value.substr(Math.max(0, value.indexOf(key.split(' ')[0])), 300)
        return <Highlighter  highlightStyle={{color:'red'}}
                             searchWords={key.split(' ').map(item=>item.trim())}
                             autoEscape={true}
                             textToHighlight={value}
        />
    }

    return (
        <div className={'wrap'} ref={wrap}>
            {!state.init ?
                <table className={'search'}>
                    <tr>
                        <img src={logo} alt={""} className={'logo'}/>
                    </tr>
                    <tr>
                        <Search
                            serverUrl={serverUrl}
                            onResultChange={(data, query) => {
                                setState({init: true, results: data.articles, total: data.total_results, query})
                            }}
                            onReset={() => {
                                setParams({offset: 0})
                                setState({page: 1})
                            }}
                            params={params}
                        />
                    </tr>
                </table> :

                <div className={'result'}>
                    <Search
                        serverUrl={serverUrl}
                        onResultChange={(data, query) => {setState({
                            init: true,
                            results: data.articles,
                            total: data.total_results,
                            query
                        })}}
                        onReset={() => {
                            setParams({offset: 0})
                            setState({page: 1})
                        }}
                        params={params}
                    />
                    <div className={'control'}>
                        <Checkbox onChange={(e) => {
                            if (e.target.checked) {
                                setParams({language: 'en'})
                            } else {
                                setParams({language: undefined})
                            }
                        }}
                        >
                            only include english result
                        </Checkbox>
                        <span style={{marginLeft: 20}}>Date:</span>
                        <DatePicker format={'YYYY-MM-DD'} onChange={(date, dateString) => setParams({date: dateString})}/>
                    </div>
                    {state.total === 0 && <Empty/>}
                    {state.total > 0 && <p>{state.total} results</p>}
                    <div>
                        {state.results.map((item, index) =>
                            <Card title={item.title}
                                  extra={item.date}
                                  className={'item'}
                                  key={index}
                                  onClick={() => window.open(item.url)}
                            >
                                <p>{getHighLightStr(item.txt, state.query)}</p>
                                <h6>{item.url}</h6>
                            </Card>
                        )}
                    </div>
                    <div className={'page'}>
                        <Pagination
                            total={state.total}
                            current={state.page}
                            onChange={page => {
                                setState({page})
                                setParams({offset: (page - 1) * 10})
                                wrap.current.scrollTop=0
                            }}
                        />
                    </div>
                </div>}
        </div>
    )
}

export default App

