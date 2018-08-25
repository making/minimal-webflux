import React, {Component} from 'react';
import {Form} from "pivotal-ui/react/forms/form";
import {Input} from "pivotal-ui/react/inputs/input";
import {Grid} from "pivotal-ui/react/flex-grids/grid";
import {FlexCol} from "pivotal-ui/react/flex-grids/flex-col";
import {PrimaryButton} from "pivotal-ui/react/buttons/buttons";
import {Divider} from "pivotal-ui/react/dividers/dividers";
import {Icon} from "pivotal-ui/react/iconography/iconography";
import axios from 'axios';
import timeago from 'timeago.js';
import 'pivotal-ui/css/border';
import 'pivotal-ui/css/box-shadows';
import 'pivotal-ui/css/alignment';
import 'pivotal-ui/css/links';
import './App.css';

function Tweet(props) {
    const ago = timeago().format(props.tweet.createdAt * 1000);
    return (
        <div className="mvxl paxl box-shadow-amb-1 tweet">
            <span className="em-high">{props.tweet.username} </span> <span
            className="type-sm type-neutral-4">{ago}</span> - {props.tweet.text}
        </div>
    );
}

export default class App extends Component {
    constructor(props) {
        super(props);
        this.state = {
            tweets: []
        };
    }

    render() {
        return (
            <div>
                <Form {...{
                    fields: {
                        username: {
                            inline: true,
                            children: <Input placeholder="Username"/>
                        },
                        text: {
                            inline: true,
                            children: <Input placeholder="Text"/>
                        }
                    },
                    onSubmit: ({initial, current}) => {
                        return axios.post("/tweets", {username: current.username, text: current.text})
                            .catch(function (error) {
                                const response = error.response;
                                if (response.status === 400) {
                                    throw response.data;
                                } else {
                                    alert("Unexpected Error!");
                                    console.log(error);
                                    throw [];
                                }
                            });
                    },
                    onSubmitError: errors => {
                        const res = {};
                        for (let i = 0; i < errors.length; i++) {
                            const error = errors[i];
                            res[error.args[0]] = error.defaultMessage;
                        }
                        return res;
                    },
                    resetOnSubmit: true
                }}>
                    {({fields, canSubmit}) => {
                        return (
                            <div>
                                <Grid>
                                    <FlexCol>{fields.username}</FlexCol>
                                    <FlexCol>{fields.text}</FlexCol>
                                    <FlexCol fixed>
                                        <PrimaryButton type="submit" disabled={!canSubmit()}>Tweet</PrimaryButton>
                                    </FlexCol>
                                </Grid>
                            </div>
                        );
                    }}
                </Form>
                {this.state.tweets.map((tweet) => <Tweet key={tweet.uuid} tweet={tweet}/>)}
                <Divider/>
                <footer>
                    <Icon src="github"/> <a href="https://github.com/making/minimal-webflux">source code</a>
                </footer>
            </div>);
    }

    componentDidMount() {
        axios.get("/tweets").then(res => {
            const tweets = res.data;
            this.setState({
                tweets: tweets
            });
            const eventSource = new EventSource('/timeline');
            eventSource.onmessage = (evt => {
                // this doesn't work well under the npm proxy ...
                const tweet = JSON.parse(evt.data);
                const tweets = this.state.tweets;
                tweets.unshift(tweet);
                while (tweets.length > 30) {
                    tweets.pop();
                }
                this.setState({tweets: tweets});
            });
        });
    }
}